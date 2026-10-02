package cz.cvut.fit.acb.harness;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * Loads the coders' classes from one build only, never from its parent, so that two builds of the same
 * classes can be loaded side by side in one JVM; everything else (the JDK, Log4j) comes from the parent
 * and is shared.
 */
final class BuildClassLoader extends URLClassLoader {

    private static final String CODER_PACKAGES = "cz.cvut.fit.acb.";

    BuildClassLoader(Path classes, ClassLoader parent) throws MalformedURLException {
        super(new URL[]{classes.toUri().toURL()}, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith(CODER_PACKAGES)) {
            return super.loadClass(name, resolve);
        }
        synchronized (this.getClassLoadingLock(name)) {
            Class<?> loaded = this.findLoadedClass(name);
            if (loaded == null) {
                loaded = this.findClass(name);
            }
            if (resolve) {
                this.resolveClass(loaded);
            }
            return loaded;
        }
    }
}
