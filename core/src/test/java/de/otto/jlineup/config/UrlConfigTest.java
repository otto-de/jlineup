package de.otto.jlineup.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlConfigTest {

    final Random random = new Random();

    //TODO: Finish this test
    @Test
    void copyOfBuilderShouldCopyAllValues() throws InvocationTargetException, IllegalAccessException {

        UrlConfig.Builder builder = UrlConfig.urlConfigBuilder();
        Method[] methods = builder.getClass().getDeclaredMethods();

        for (Method method : methods) {
            if (method.getName().startsWith("with")) {
                //System.out.println("Invoking method: " + method.getName());
                Class<?>[] paramTypes = method.getParameterTypes();
                //Type[] genericParameterTypes = method.getGenericParameterTypes();
                //System.err.println(genericParameterTypes);
                Object value = getDummyValue(paramTypes[0]);
                method.invoke(builder, value);
            }
        }
        UrlConfig originalConfig = builder.build();

        //Given
//        UrlConfig originalConfig = UrlConfig.urlConfigBuilder()
//                .withAlternatingCookies(ImmutableList.of(ImmutableList.of(new Cookie("name1", "value1")), ImmutableList.of(new Cookie("name2", "value2"))))
//                .withCookies(ImmutableList.of(new Cookie("name", "value")))
//                .withCleanupPaths(ImmutableList.of("path"))
//                .withPaths(ImmutableList.of("path"))
//                .withDevices(ImmutableList.of(DeviceConfig.deviceConfig(1, 1)))
//                .build();

        //When
        UrlConfig copiedConfig = UrlConfig.copyOfBuilder(originalConfig).build();

        //Then
        assertEquals(originalConfig, copiedConfig);

    }

    @Test
    void sanitizeShouldSanitize() {

    }

    private Object getDummyValue(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) return true;
        if (type == int.class || type == Integer.class) return random.nextInt();
        if (type == float.class || type == Float.class) return random.nextFloat();
        if (type == double.class || type == Double.class) return random.nextDouble();
        if (type == String.class) return random.nextInt() + "_testString";
        if (type == List.class) return Collections.emptyList();
        //if (type == Map.class) return Collections.emptyMap();
        if (type == Set.class) return Collections.emptySet();
        //Check the generic type of the Map and create a dummy map accordingly
        if (type == Map.class) {
            Map<Object, Object> map = new HashMap<>();
            map.put(getDummyValue(Object.class), getDummyValue(Object.class));
            return map;
        }
        if (type == Cookie.class) return new Cookie(random.nextInt(5) + "_name", random.nextInt(5) + "_value");
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.out.printf("Not testing type: %s%n in UrlConfigTest", type.getName());
            throw new RuntimeException(e);
        }
    }
}