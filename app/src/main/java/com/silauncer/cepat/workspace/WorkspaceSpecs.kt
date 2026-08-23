package com.silauncer.cepat.workspace

import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.util.Xml
import com.silauncer.cepat.R
import com.silauncer.cepat.util.ResourceHelper
import java.io.IOException
import kotlin.math.roundToInt
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

private const val TAG = "WorkspaceSpecs"

/**
 * Parses and holds responsive workspace specifications from XML resources.
 *
 * // [Jalur Class]: com.silauncer.cepat.workspace.WorkspaceSpecs
 * // [Penjelasan]: Kelas pengurai berkas spesifikasi responsif untuk workspace (seperti margins, paddings, gutter, dan cell sizes) guna menentukan dimensi grid yang ideal di runtime.
 */
class WorkspaceSpecs(resourceHelper: ResourceHelper) {
    object XmlTags {
        const val WORKSPACE_SPECS = "workspaceSpecs"
        const val WORKSPACE_SPEC = "workspaceSpec"
        const val START_PADDING = "startPadding"
        const val END_PADDING = "endPadding"
        const val GUTTER = "gutter"
        const val CELL_SIZE = "cellSize"
    }

    val workspaceHeightSpecList = mutableListOf<WorkspaceSpec>()
    val workspaceWidthSpecList = mutableListOf<WorkspaceSpec>()

    init {
        // // [Jalur Class]: com.silauncer.cepat.workspace.WorkspaceSpecs
        // // [Penjelasan]: Blok inisialisasi yang membaca XML spesifikasi workspace, mengekstrak tipe spesifikasi, padding, gutter, dan cellSize untuk disimpan dalam daftar spesifikasi tinggi/lebar.
        try {
            val parser: XmlResourceParser = resourceHelper.getXml()
            val depth = parser.depth
            var type: Int
            while (
                (parser.next().also { type = it } != XmlPullParser.END_TAG ||
                    parser.depth > depth) && type != XmlPullParser.END_DOCUMENT
            ) {
                if (type == XmlPullParser.START_TAG && XmlTags.WORKSPACE_SPECS == parser.name) {
                    val displayDepth = parser.depth
                    while (
                        (parser.next().also { type = it } != XmlPullParser.END_TAG ||
                            parser.depth > displayDepth) && type != XmlPullParser.END_DOCUMENT
                    ) {
                        if (
                            type == XmlPullParser.START_TAG && XmlTags.WORKSPACE_SPEC == parser.name
                        ) {
                            val attrs =
                                resourceHelper.obtainStyledAttributes(
                                    Xml.asAttributeSet(parser),
                                    R.styleable.WorkspaceSpec
                                )
                            val maxAvailableSize =
                                attrs.getDimensionPixelSize(
                                    R.styleable.WorkspaceSpec_maxAvailableSize,
                                    0
                                )
                            val specType =
                                WorkspaceSpec.SpecType.values()[
                                        attrs.getInt(
                                            R.styleable.WorkspaceSpec_specType,
                                            WorkspaceSpec.SpecType.HEIGHT.ordinal
                                        )]
                            attrs.recycle()

                            var startPadding: SizeSpec? = null
                            var endPadding: SizeSpec? = null
                            var gutter: SizeSpec? = null
                            var cellSize: SizeSpec? = null

                            val limitDepth = parser.depth
                            while (
                                (parser.next().also { type = it } != XmlPullParser.END_TAG ||
                                    parser.depth > limitDepth) && type != XmlPullParser.END_DOCUMENT
                            ) {
                                val attr: AttributeSet = Xml.asAttributeSet(parser)
                                if (type == XmlPullParser.START_TAG) {
                                    when (parser.name) {
                                        XmlTags.START_PADDING -> {
                                            startPadding = SizeSpec(resourceHelper, attr)
                                        }
                                        XmlTags.END_PADDING -> {
                                            endPadding = SizeSpec(resourceHelper, attr)
                                        }
                                        XmlTags.GUTTER -> {
                                            gutter = SizeSpec(resourceHelper, attr)
                                        }
                                        XmlTags.CELL_SIZE -> {
                                            cellSize = SizeSpec(resourceHelper, attr)
                                        }
                                    }
                                }
                            }

                            if (
                                startPadding == null ||
                                    endPadding == null ||
                                    gutter == null ||
                                    cellSize == null
                            ) {
                                throw IllegalStateException(
                                    "All attributes in workspaceSpec must be defined"
                                )
                            }

                            val workspaceSpec =
                                WorkspaceSpec(
                                    maxAvailableSize,
                                    specType,
                                    startPadding,
                                    endPadding,
                                    gutter,
                                    cellSize
                                )
                            if (workspaceSpec.isValid()) {
                                if (workspaceSpec.specType == WorkspaceSpec.SpecType.HEIGHT)
                                    workspaceHeightSpecList.add(workspaceSpec)
                                else workspaceWidthSpecList.add(workspaceSpec)
                            } else {
                                throw IllegalStateException("Invalid workspaceSpec found.")
                            }
                        }
                    }

                    if (workspaceWidthSpecList.isEmpty() || workspaceHeightSpecList.isEmpty()) {
                        throw IllegalStateException(
                            "WorkspaceSpecs is incomplete - " +
                                "height list size = ${workspaceHeightSpecList.size}; " +
                                "width list size = ${workspaceWidthSpecList.size}."
                        )
                    }
                }
            }
            parser.close()
        } catch (e: Exception) {
            when (e) {
                is IOException,
                is XmlPullParserException -> {
                    throw RuntimeException("Failure parsing workspaces specs file.", e)
                }
                else -> throw e
            }
        }
    }

    /**
     * Returns the CalculatedWorkspaceSpec for width, based on the available width and the
     * WorkspaceSpecs.
     */
    fun getCalculatedWidthSpec(columns: Int, availableWidth: Int): CalculatedWorkspaceSpec {
        // // [Jalur Class]: com.silauncer.cepat.workspace.WorkspaceSpecs
        // // [Penjelasan]: Mengambil spesifikasi lebar pertama yang memiliki maxAvailableSize lebih besar dari atau sama dengan lebar layar yang tersedia saat ini.
        val widthSpec = workspaceWidthSpecList.first { availableWidth <= it.maxAvailableSize }

        return CalculatedWorkspaceSpec(availableWidth, columns, widthSpec)
    }

    /**
     * Returns the CalculatedWorkspaceSpec for height, based on the available height and the
     * WorkspaceSpecs.
     */
    fun getCalculatedHeightSpec(rows: Int, availableHeight: Int): CalculatedWorkspaceSpec {
        // // [Jalur Class]: com.silauncer.cepat.workspace.WorkspaceSpecs
        // // [Penjelasan]: Mengambil spesifikasi tinggi pertama yang memiliki maxAvailableSize lebih besar dari atau sama dengan tinggi layar yang tersedia saat ini.
        val heightSpec = workspaceHeightSpecList.first { availableHeight <= it.maxAvailableSize }

        return CalculatedWorkspaceSpec(availableHeight, rows, heightSpec)
    }
}

class CalculatedWorkspaceSpec(
    val availableSpace: Int,
    val cells: Int,
    val workspaceSpec: WorkspaceSpec
) {
    var startPaddingPx: Int = 0
        private set
    var endPaddingPx: Int = 0
        private set
    var gutterPx: Int = 0
        private set
    var cellSizePx: Int = 0
        private set
    init {
        // // [Jalur Class]: com.silauncer.cepat.workspace.CalculatedWorkspaceSpec
        // // [Penjelasan]: Menghitung padding awal, padding akhir, gutter, dan ukuran sel berdasarkan spesifikasi porsi statis, relatif terhadap ruang tersedia, atau sisa ruang layar.

        // Calculate all fixed size first
        if (workspaceSpec.startPadding.fixedSize > 0)
            startPaddingPx = workspaceSpec.startPadding.fixedSize.roundToInt()
        if (workspaceSpec.endPadding.fixedSize > 0)
            endPaddingPx = workspaceSpec.endPadding.fixedSize.roundToInt()
        if (workspaceSpec.gutter.fixedSize > 0)
            gutterPx = workspaceSpec.gutter.fixedSize.roundToInt()
        if (workspaceSpec.cellSize.fixedSize > 0)
            cellSizePx = workspaceSpec.cellSize.fixedSize.roundToInt()

        // Calculate all available space next
        if (workspaceSpec.startPadding.ofAvailableSpace > 0)
            startPaddingPx =
                (workspaceSpec.startPadding.ofAvailableSpace * availableSpace).roundToInt()
        if (workspaceSpec.endPadding.ofAvailableSpace > 0)
            endPaddingPx = (workspaceSpec.endPadding.ofAvailableSpace * availableSpace).roundToInt()
        if (workspaceSpec.gutter.ofAvailableSpace > 0)
            gutterPx = (workspaceSpec.gutter.ofAvailableSpace * availableSpace).roundToInt()
        if (workspaceSpec.cellSize.ofAvailableSpace > 0)
            cellSizePx = (workspaceSpec.cellSize.ofAvailableSpace * availableSpace).roundToInt()

        // Calculate remainder space last
        val gutters = cells - 1
        val usedSpace = startPaddingPx + endPaddingPx + (gutterPx * gutters) + (cellSizePx * cells)
        val remainderSpace = availableSpace - usedSpace
        if (workspaceSpec.startPadding.ofRemainderSpace > 0)
            startPaddingPx =
                (workspaceSpec.startPadding.ofRemainderSpace * remainderSpace).roundToInt()
        if (workspaceSpec.endPadding.ofRemainderSpace > 0)
            endPaddingPx = (workspaceSpec.endPadding.ofRemainderSpace * remainderSpace).roundToInt()
        if (workspaceSpec.gutter.ofRemainderSpace > 0)
            gutterPx = (workspaceSpec.gutter.ofRemainderSpace * remainderSpace).roundToInt()
        if (workspaceSpec.cellSize.ofRemainderSpace > 0)
            cellSizePx = (workspaceSpec.cellSize.ofRemainderSpace * remainderSpace).roundToInt()
    }
}

data class WorkspaceSpec(
    val maxAvailableSize: Int,
    val specType: SpecType,
    val startPadding: SizeSpec,
    val endPadding: SizeSpec,
    val gutter: SizeSpec,
    val cellSize: SizeSpec
) {

    enum class SpecType {
        HEIGHT,
        WIDTH
    }

    fun isValid(): Boolean {
        // // [Jalur Class]: com.silauncer.cepat.workspace.WorkspaceSpec
        // // [Penjelasan]: Memvalidasi bahwa maxAvailableSize > 0 dan seluruh spesifikasi ukuran (padding, gutter, cellSize) masing-masing valid.
        if (maxAvailableSize <= 0) {
            Log.e(TAG, "WorkspaceSpec#isValid - maxAvailableSize <= 0")
            return false
        }

        // All specs need to be individually valid
        if (!allSpecsAreValid()) {
            Log.e(TAG, "WorkspaceSpec#isValid - !allSpecsAreValid()")
            return false
        }

        return true
    }

    private fun allSpecsAreValid(): Boolean =
        startPadding.isValid() && endPadding.isValid() && gutter.isValid() && cellSize.isValid()
}

class SizeSpec(resourceHelper: ResourceHelper, attrs: AttributeSet) {
    val fixedSize: Float
    val ofAvailableSpace: Float
    val ofRemainderSpace: Float

    init {
        // // [Jalur Class]: com.silauncer.cepat.workspace.SizeSpec
        // // [Penjelasan]: Mengekstrak nilai fixedSize, ofAvailableSpace, dan ofRemainderSpace dari resource menggunakan ResourceHelper.
        val styledAttrs = resourceHelper.obtainStyledAttributes(attrs, R.styleable.SpecSize)

        fixedSize = getValue(styledAttrs, R.styleable.SpecSize_fixedSize)
        ofAvailableSpace = getValue(styledAttrs, R.styleable.SpecSize_ofAvailableSpace)
        ofRemainderSpace = getValue(styledAttrs, R.styleable.SpecSize_ofRemainderSpace)

        styledAttrs.recycle()
    }

    private fun getValue(a: TypedArray, index: Int): Float {
        // // [Jalur Class]: com.silauncer.cepat.workspace.SizeSpec
        // // [Penjelasan]: Mengonversi tipe nilai atribut bertipe TypedValue.TYPE_DIMENSION atau TYPE_FLOAT menjadi Float.
        if (a.getType(index) == TypedValue.TYPE_DIMENSION) {
            return a.getDimensionPixelSize(index, 0).toFloat()
        } else if (a.getType(index) == TypedValue.TYPE_FLOAT) {
            return a.getFloat(index, 0f)
        }
        return 0f
    }

    fun isValid(): Boolean {
        // // [Jalur Class]: com.silauncer.cepat.workspace.SizeSpec
        // // [Penjelasan]: Memverifikasi bahwa hanya ada maksimal satu spesifikasi jenis ukuran (fixed, relative available, atau relative remainder) yang diisi dan nilainya valid.
        // All attributes are empty
        if (fixedSize < 0f && ofAvailableSpace <= 0f && ofRemainderSpace <= 0f) {
            Log.e(TAG, "SizeSpec#isValid - all attributes are empty")
            return false
        }

        // More than one attribute is filled
        val attrCount =
            (if (fixedSize > 0) 1 else 0) +
                (if (ofAvailableSpace > 0) 1 else 0) +
                (if (ofRemainderSpace > 0) 1 else 0)
        if (attrCount > 1) {
            Log.e(TAG, "SizeSpec#isValid - more than one attribute is filled")
            return false
        }

        // Values should be between 0 and 1
        if (ofAvailableSpace !in 0f..1f || ofRemainderSpace !in 0f..1f) {
            Log.e(TAG, "SizeSpec#isValid - values should be between 0 and 1")
            return false
        }

        return true
    }

    override fun toString(): String {
        return "SizeSpec(fixedSize=$fixedSize, ofAvailableSpace=$ofAvailableSpace, " +
            "ofRemainderSpace=$ofRemainderSpace)"
    }
}
