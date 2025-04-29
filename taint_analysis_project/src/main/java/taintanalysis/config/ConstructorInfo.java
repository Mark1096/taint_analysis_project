package taintanalysis.config;

import java.util.List;

/**
 * <h1> ConstructorInfo </h1>
 *
 * This class contains the list of parameter types that are passed to the constructor.
 */
public class ConstructorInfo {
    private List<String> parameterTypes;

    /**
     * Returns the list of parameter types.
     *
     * @return list string
     */
    public List<String> getParameterTypes() {
        return parameterTypes;
    }

}

