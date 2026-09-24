package cc.nexusdev.trails.quest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Small reflective boundary keeps optional plugin APIs out of the runtime dependency graph. */
final class OptionalPluginAccess {
    private OptionalPluginAccess() {}

    static Class<?> type(String pluginName, String className) throws ReflectiveOperationException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null || !plugin.isEnabled()) throw new IllegalStateException(pluginName + " is unavailable");
        return Class.forName(className, true, plugin.getClass().getClassLoader());
    }

    static Object call(Object receiver, String name, Object... args) throws ReflectiveOperationException {
        Class<?> type = receiver instanceof Class<?> c ? c : receiver.getClass();
        Method method = Arrays.stream(type.getMethods()).filter(m -> m.getName().equals(name)
                && m.getParameterCount() == args.length && matches(m.getParameterTypes(), args))
                .findFirst().orElseThrow(() -> new NoSuchMethodException(type.getName() + "." + name));
        try {
            return method.invoke(receiver instanceof Class<?> ? null : receiver, args);
        } catch (InvocationTargetException ex) {
            throw new ReflectiveOperationException(name + " failed", ex.getCause());
        }
    }

    private static boolean matches(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i] == int.class ? Integer.class : types[i];
            if (args[i] != null && !type.isInstance(args[i])) return false;
        }
        return true;
    }
}
