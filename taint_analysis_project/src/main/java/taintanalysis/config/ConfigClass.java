package taintanalysis.config;

import java.util.List;

/**
 * <h1> ConfigClass </h1>
 *
 * This class contains information about the classes included in the external sources.
 */
public class ConfigClass {
    private String className;
    private List<String> methods;
    private List<ConstructorInfo> constructors;

    /**
     * Returns the name of the class.
     *
     * @return string
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the methods belonging to the class.
     *
     * @return string
     */
    public List<String> getMethods() {
        return methods;
    }

    /**
     * Returns the list of constructors of the class.
     *
     * @return list constructor info
     */
    public List<ConstructorInfo> getConstructors() {
        return constructors;
    }

}

