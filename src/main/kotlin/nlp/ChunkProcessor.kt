package nlp

import java.lang.IllegalStateException

class ChunkProcessor {

    var verbose: Boolean = false;
    private var sentence: Sentence? = null;
    private var currentChunk: Chunk? = null;
    private var currentChunkState: ChunkState = ChunkState.NULL;
    private var chunkList: MutableList<Chunk> = mutableListOf();

    internal fun processChunks(sentence: Sentence) {
        this.sentence = sentence;

        for ((i, chunk) in sentence.chunksAsStrings!!.withIndex()) {
            println("Processing chunk ${i};");
            when (currentChunkState) {
                ChunkState.BEGINNING -> processChunkWhileAtBeginning(chunk, currentChunk, i)
                ChunkState.MIDDLE -> processChunkWhileInMiddle(chunk, currentChunk, i)
                ChunkState.NULL -> processFromStart(chunk, i)
                ChunkState.NON_CHUNK -> processChunkWhileOnNonChunk(chunk, i)
            }
        }

        sentence.chunkList = chunkList;

    }

    private fun processChunkWhileOnNonChunk(chunk: String, i: Int) {
        when {
            chunk.startsWith("B") -> {
                markStartChunk(i)
                currentChunkState = ChunkState.BEGINNING;
            }
            chunk.startsWith("I") -> {
                throw IllegalStateException()
            }
            chunk.startsWith("O") -> {
                markChunkOfLengthOne(i)
                currentChunkState = ChunkState.NON_CHUNK;
            }
            else -> {
                throw IllegalStateException();
            }
        }
        return
    }

    private fun processChunkWhileInMiddle(chunk: String, currentChunk: Chunk?, i: Int) {
        when {
            chunk.startsWith("B") -> {
                markEndOfChunk();
                markStartChunk(i)
                currentChunkState = ChunkState.BEGINNING;
            }
            chunk.startsWith("I") -> {
                addTokenAndTagAt(currentChunk, i)
                currentChunkState = ChunkState.MIDDLE;
            }
            chunk.startsWith("O") -> {
                markEndOfChunk();
                markChunkOfLengthOne(i)
                currentChunkState = ChunkState.NON_CHUNK;
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
            markStartChunk(i)
            currentChunkState = ChunkState.BEGINNING;
        } else if (chunk.startsWith("I")) {
            addTokenAndTagAt(currentChunk, i)
            currentChunkState = ChunkState.MIDDLE;
        } else if (chunk.startsWith("O")) {
            markEndOfChunk();
            markChunkOfLengthOne(i)
            currentChunkState = ChunkState.NON_CHUNK;
        } else {
            throw IllegalStateException();
        }

        return
    }

    /**
     * TODO
     */
    private fun processFromStart(chunk: String, i: Int) {
        when {
            chunk.startsWith("B") -> {
                markStartChunk(i)
                ChunkState.BEGINNING;
            }
            chunk.startsWith("I") -> throw IllegalStateException()
            chunk.startsWith("O") -> {
                markChunkOfLengthOne(i)
                ChunkState.NON_CHUNK;
            }
            else -> throw IllegalStateException()
        }.also { currentChunkState = it }
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
        currentChunk!!.addTokenTag(sentence!!.tagsAsStringArray!![i], sentence!!.tokensAsStringArray!![i])
    }

}
