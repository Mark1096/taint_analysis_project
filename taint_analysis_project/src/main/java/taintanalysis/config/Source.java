package taintanalysis.config;

import java.util.List;

/**
 * <h1> Source </h1>
 *
 * It is used to record information about an external source.
 */
public class Source {
    private String name;
    private boolean trusted;
    private List<ConfigClass> classes;

    /**
     * Returns the name of the external source.
     *
     * @return string
     */
    public String getName() {
        return name;
    }

    /**
     * Checks whether the external source is trusted.
     *
     * @return boolean
     */
    public boolean isTrusted() {
        return trusted;
    }

    /**
     * Returns the list of classes included in the external source.
     *
     * @return list config class
     */
    public List<ConfigClass> getClasses() {
        return classes;
    }

}

