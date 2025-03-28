package ru.snilov.modu.rpc.client;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import ru.snilov.modu.rpc.api.ModuRpcApi;

import java.util.*;
import java.util.stream.Collectors;

public class ModuRpcClientBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(ModuRpcClientBeanDefinitionRegistryPostProcessor.class);

    private final Environment environment;
    private ApplicationContext applicationContext;

    public ModuRpcClientBeanDefinitionRegistryPostProcessor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (applicationContext != null) {
            // Получаем доступ к свойствам modurpc.clients
            Map<String, String> urlMap = getPropertiesByPattern("modurpc.clients", ".url");

            String basePackage = getBasePackage(applicationContext);
            List<String> extraPackages = getExtraScanPackages();

            List<String> scanPackages = new ArrayList<>();
            if (basePackage != null) {
                scanPackages.add(basePackage);
            }
            scanPackages.addAll(extraPackages);

            logger.info("Scanning RPC clients in packages: {}", scanPackages);

            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages(scanPackages.toArray(String[]::new))
                    .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes));

            Set<Class<?>> rpcApiClasses = reflections.getTypesAnnotatedWith(ModuRpcApi.class);

            Set<Class<?>> rpcApiInterfaces = rpcApiClasses.stream()
                    .filter(Class::isInterface)
                    .collect(Collectors.toSet());

            ModuRpcClientMethodInterceptor interceptor = applicationContext.getBean(ModuRpcClientMethodInterceptor.class);

            for (Class<?> apiInterface : rpcApiInterfaces) {
                if (hasImplementation(registry, apiInterface)) {
                    continue;
                }

                if (!urlMap.containsKey(apiInterface.getPackageName())) {
                    throw new IllegalArgumentException("Url for interface [%s] not found. Please set [modurpc.clients.%s.url] in application.properties"
                            .formatted(apiInterface.getName(), apiInterface.getPackageName()));
                }

                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(ModuRpcClientFactoryBean.class);
                beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(apiInterface);
                beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(urlMap.get(apiInterface.getPackageName()));
                beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(interceptor);

                // Помечаем информацией, что это сгенерированный бин
                beanDefinition.setAttribute("modurpc.client", true);

                registry.registerBeanDefinition(apiInterface.getSimpleName(), beanDefinition);
                logger.info("registered rpc client for [{}]", apiInterface.getName());
            }
        }
    }

    // Определение пакета от класса аннотированный SpringBootApplication
    private String getBasePackage(ApplicationContext context) {
        if (context != null) {
            for (String beanName : context.getBeanDefinitionNames()) {
                Class<?> beanClass = context.getType(beanName);
                if (beanClass != null && beanClass.isAnnotationPresent(SpringBootApplication.class)) {
                    return beanClass.getPackageName();
                }
            }
        }

        logger.warn("Cannot determine base package. Please specify 'modurpc.scan.packages' manually.");
        return null;
    }

    // Пользователь может указать дополнительные пакеты для сканирования
    private List<String> getExtraScanPackages() {
        String property = environment.getProperty("modurpc.scan.packages");
        if (property == null) {
            return List.of();
        }
        return Arrays.stream(property.split(","))
                .map(String::trim)
                .filter(pkg -> !pkg.isEmpty())
                .distinct()
                .toList();
    }

    // Добываем из переменных окружений URL назначения запроса
    private Map<String, String> getPropertiesByPattern(String propertyBaseName, String suffix) {
        Binder binder = Binder.get(environment);
        return binder.bind(propertyBaseName, Bindable.mapOf(String.class, String.class))
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().endsWith(suffix))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(0, entry.getKey().length() - suffix.length()),
                        Map.Entry::getValue
                ));
    }

    private boolean hasImplementation(BeanDefinitionRegistry registry, Class<?> apiInterface) {
        String[] beanNames = ((ConfigurableListableBeanFactory) registry).getBeanNamesForType(apiInterface);
        return beanNames.length > 0;
    }
}