package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.modifier.SyntheticState;
import net.bytebuddy.modifier.TypeVisibility;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An auxiliary type that provides services to the instrumentation of another type. Implementations should provide
 * meaningful {@code equals(Object)} and {@code hashCode()} implementations in order to avoid multiple creations
 * of this type.
 */
public interface AuxiliaryType {

    /**
     * The default type access of an auxiliary type.
     */
    static final List<ModifierContributor.ForType> DEFAULT_TYPE_MODIFIER = Collections.unmodifiableList(
            Arrays.<ModifierContributor.ForType>asList(TypeVisibility.PACKAGE_PRIVATE, SyntheticState.SYNTHETIC));

    /**
     * Creates a new auxiliary type.
     *
     * @param auxiliaryTypeName     The fully qualified non-internal name for this auxiliary type. The type should be in
     *                              the same package than the instrumented type this auxiliary type is providing services
     *                              to in order to allow package-private access.
     * @param classFileVersion      The class file version the auxiliary class should be written in.
     * @param methodAccessorFactory A factory for accessor methods.
     * @return A dynamically created type representing this auxiliary type.
     */
    DynamicType make(String auxiliaryTypeName,
                     ClassFileVersion classFileVersion,
                     MethodAccessorFactory methodAccessorFactory);

    /**
     * A factory for creating method proxies for an auxiliary type. Such proxies are required to allow a type to
     * call methods of a second type that are usually not accessible for the first type. This strategy is also adapted
     * by the Java compiler that creates accessor methods for example to implement inner classes.
     */
    static interface MethodAccessorFactory {

        static interface LookupMode {

            static enum Default implements LookupMode {

                BY_SIGNATURE {
                    @Override
                    public MethodDescription resolve(MethodDescription targetMethod,
                                                     MethodLookupEngine.Finding finding,
                                                     Map<String, MethodDescription> invokableMethods) {
                        MethodDescription resolvedMethod = invokableMethods.get(targetMethod.getUniqueSignature());
                        if (resolvedMethod == null) {
                            throw new IllegalArgumentException(String.format("Method %s is not reachable from %s",
                                    targetMethod, finding.getLookedUpType()));
                        }
                        return resolvedMethod;
                    }
                },

                EXACT {
                    @Override
                    public MethodDescription resolve(MethodDescription targetMethod,
                                                     MethodLookupEngine.Finding finding,
                                                     Map<String, MethodDescription> reachableMethods) {
                        return targetMethod;
                    }
                }
            }

            MethodDescription resolve(MethodDescription targetMethod,
                                      MethodLookupEngine.Finding finding,
                                      Map<String, MethodDescription> reachableMethods);
        }

        /**
         * Requests a new accessor method for the requested method. If such a method cannot be created, an exception
         * will be thrown.
         *
         * @param targetMethod The target method for which an accessor method is required.
         * @return A new accessor method.
         */
        MethodDescription requireAccessorMethodFor(MethodDescription targetMethod, LookupMode lookupMode);
    }
}
