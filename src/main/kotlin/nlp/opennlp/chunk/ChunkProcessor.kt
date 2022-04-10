package nlp.opennlp.chunk

import java.lang.IllegalStateException

/**
 * TODO
 */
class ChunkProcessor {

    var verbose: Boolean = false;
    private var sentence: Sentence? = null;
    private var currentChunk: Chunk? = null;
    private var currentChunkProcessingState: ChunkProcessingState = ChunkProcessingState.NULL;
    private var chunkList: MutableList<Chunk> = mutableListOf();

    internal fun processChunks(sentence: Sentence, chunksAsStrings: List<String>) {
        this.sentence = sentence;

        for ((i, chunk) in chunksAsStrings.withIndex()) {

            if (verbose) {
                println("Processing chunk ${i};");
            }

            when (currentChunkProcessingState) {
                ChunkProcessingState.BEGINNING -> processChunkWhileAtBeginning(chunk, currentChunk, i)
                ChunkProcessingState.MIDDLE -> processChunkWhileInMiddle(chunk, currentChunk, i)
                ChunkProcessingState.NULL -> processFromStart(chunk, i)
                ChunkProcessingState.NON_CHUNK -> processChunkWhileOnNonChunk(chunk, i)
            }
        }

        sentence.chunkList = chunkList;

    }

    private fun processChunkWhileOnNonChunk(chunk: String, i: Int) {
        when {
            chunk.startsWith("B") -> markBeginningOfChunk(chunk, i)
            chunk.startsWith("I") -> throw IllegalStateException()
            chunk.startsWith("O") -> {
                markChunkOfLengthOne(i)
                currentChunkProcessingState = ChunkProcessingState.NON_CHUNK;
            }
            else -> throw IllegalStateException()
        }
        return
    }

    /**
     * TODO
     */
    private fun processChunkWhileInMiddle(chunk: String, currentChunk: Chunk?, i: Int) {
        when {
            chunk.startsWith("B") -> {
                markEndOfChunk();
                markBeginningOfChunk(chunk, i)
            }
            chunk.startsWith("I") -> {
                addTokenAndTagAt(currentChunk, i)
                currentChunkProcessingState = ChunkProcessingState.MIDDLE;
            }
            chunk.startsWith("O") -> {
                markEndOfChunk();
                markChunkOfLengthOne(i)
                currentChunkProcessingState = ChunkProcessingState.NON_CHUNK;
            }
            else -> {
                throw IllegalStateException();
            }
        }
        return
    }

    private fun processChunkWhileAtBeginning(chunk: String, currentChunk: Chunk?, i: Int) {
        if (chunk.startsWith("B")) {
            markEndOfChunk();
            markBeginningOfChunk(chunk, i)
        } else if (chunk.startsWith("I")) {
            addTokenAndTagAt(currentChunk, i)
            currentChunkProcessingState = ChunkProcessingState.MIDDLE;
        } else if (chunk.startsWith("O")) {
            markEndOfChunk();
            markChunkOfLengthOne(i)
            currentChunkProcessingState = ChunkProcessingState.NON_CHUNK;
        } else {
            throw IllegalStateException();
        }

        return
    }

    private fun markBeginningOfChunk(chunk: String, i: Int) {
        markStartChunk(i)
        currentChunk!!.partOfSpeech = chunk.removePrefix("B-")
        currentChunkProcessingState = ChunkProcessingState.BEGINNING;
    }

    /**
     * TODO
     */
    private fun processFromStart(chunk: String, i: Int) {
        when {
            chunk.startsWith("B") -> {
                markBeginningOfChunk(chunk, i);
            }
            chunk.startsWith("I") -> throw IllegalStateException()
            chunk.startsWith("O") -> {
                markChunkOfLengthOne(i)
                currentChunkProcessingState = ChunkProcessingState.NON_CHUNK;
            }
            else -> throw IllegalStateException()
        }
    }

    private fun markChunkOfLengthOne(i: Int) {
        markStartChunk(i)
        markEndOfChunk();
    }

    private fun markStartChunk(i: Int) {
        currentChunk = Chunk()
        addTokenAndTagAt(currentChunk, i)
    }

    private fun markEndOfChunk() {
        chunkList.add(currentChunk!!);
        currentChunk = null;
    }

    private fun addTokenAndTagAt(currentChunk: Chunk?, i: Int) {
        currentChunk!!.addTokenTag(sentence.getTagStringAt(i), sentence.getTokenStringAt(i))
    }

    private fun Sentence?.getTokenStringAt(i: Int) = this!!.tokens!![i].tokenString

    private fun Sentence?.getTagStringAt(i: Int) = this!!.tags!![i].tagString

}
