package tk.sciwhiz12.gradle.license

import groovy.transform.PackageScope
import org.cadixdev.gradle.licenser.LicenseExtension
import org.cadixdev.gradle.licenser.LicenseProperties
import org.cadixdev.gradle.licenser.Licenser

import java.util.function.BiConsumer

@PackageScope
class LicenserMetaClass extends DelegatingMetaClass {
    final BiConsumer<LicenseProperties, Map<String, String>> propertiesEditor

    LicenserMetaClass(MetaClass metaClass, BiConsumer<LicenseProperties, Map<String, String>> propertiesEditor) {
        super(metaClass)
        this.propertiesEditor = propertiesEditor
    }

    @Override
    Object invokeStaticMethod(Object object, String methodName, Object[] arguments) {
        if (methodName == "prepareHeader" && arguments.length == 2 &&
                arguments[0] instanceof LicenseExtension && arguments[1] instanceof LicenseProperties) {
            return LicenserExtensions.prepareHeader(propertiesEditor,
                    arguments[0] as LicenseExtension, arguments[1] as LicenseProperties)
        }
        return super.invokeStaticMethod(object, methodName, arguments)
    }

    static void interceptPrepareHeader(BiConsumer<LicenseProperties, Map<String, String>> propertiesEditor) {
        def metaClass = new LicenserMetaClass(Licenser.metaClass, propertiesEditor)
        metaClass.initialize()

        Licenser.metaClass = metaClass
    }
}
