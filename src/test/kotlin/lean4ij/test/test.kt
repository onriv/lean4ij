package lean4ij.test

import com.google.common.io.Resources
import java.nio.charset.StandardCharsets

fun readResource(s: String) : String = Resources.toString(Resources.getResource(s), StandardCharsets.UTF_8)