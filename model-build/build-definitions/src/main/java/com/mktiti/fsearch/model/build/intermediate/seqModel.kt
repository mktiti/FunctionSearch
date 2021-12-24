import com.mktiti.fsearch.model.build.intermediate.*
import java.util.stream.Stream
import kotlin.streams.toList

private data class SimpleTypeInfoSeqResult(
        override val directInfos: Stream<SemiInfo.DirectInfo>,
        override val templateInfos: Stream<SemiInfo.TemplateInfo>
) : TypeInfoSeqResult {
    override fun collect(): TypeInfoResult = TypeInfoResult(directInfos.toList(), templateInfos.toList())
}

interface TypeInfoSeqResult {
    val directInfos: Stream<SemiInfo.DirectInfo>
    val templateInfos: Stream<SemiInfo.TemplateInfo>

    companion object {
        fun simple(directInfos: Stream<SemiInfo.DirectInfo>, templateInfos: Stream<SemiInfo.TemplateInfo>): TypeInfoSeqResult = SimpleTypeInfoSeqResult(directInfos, templateInfos)
    }

    fun collect(): TypeInfoResult

}

private data class SimpleFunctionInfoSeqResult(
        override val staticFunctions: Stream<RawFunInfo>,
        override val instanceMethods: Stream<IntInstanceFunEntry>
) : FunctionInfoSeqResult {
    override fun collect() = FunctionInfoResult(staticFunctions.toList(), instanceMethods.toList())
}

interface FunctionInfoSeqResult {
    val staticFunctions: Stream<RawFunInfo>
    val instanceMethods: Stream<IntInstanceFunEntry>

    companion object {
        fun simple(staticFunctions: Stream<RawFunInfo>, instanceMethods: Stream<IntInstanceFunEntry>): FunctionInfoSeqResult
            = SimpleFunctionInfoSeqResult(staticFunctions, instanceMethods)
    }

    fun collect(): FunctionInfoResult

}

private data class SimpleArtifactInfoSeqResult(
        override val typeInfo: TypeInfoSeqResult,
        override val funInfo: FunctionInfoSeqResult
) : ArtifactInfoSeqResult {
    override fun collect() = ArtifactInfoResult(typeInfo.collect(), funInfo.collect())
}

interface ArtifactInfoSeqResult {
    val typeInfo: TypeInfoSeqResult
    val funInfo: FunctionInfoSeqResult

    companion object {
        fun simple(typeInfo: TypeInfoSeqResult, funInfo: FunctionInfoSeqResult): ArtifactInfoSeqResult
            = SimpleArtifactInfoSeqResult(typeInfo, funInfo)
    }

    fun collect(): ArtifactInfoResult

}