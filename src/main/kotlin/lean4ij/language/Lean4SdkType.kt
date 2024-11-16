package lean4ij.language

import com.intellij.openapi.projectRoots.AdditionalDataConfigurable
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.SdkType
import org.jdom.Element

class Lean4SdkType : SdkType("Lean4") {

    companion object {
        val INSTANCE = Lean4SdkType()
    }

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {
    }

    override fun suggestHomePath(): String? {
        return null
    }

    override fun isValidSdkHome(path: String): Boolean {
        return true
    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        return sdkHome
    }

    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator
    ): AdditionalDataConfigurable? {
        return null
    }

    override fun getPresentableName(): String {
        return "Lean4"
    }
}