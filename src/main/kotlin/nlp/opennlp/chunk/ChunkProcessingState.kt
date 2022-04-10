package nlp.opennlp.chunk

/**
 * A state for a chunk processor to be in, representing different stages of an FSM (finite state machine).
 */
enum class ChunkProcessingState {

    /**
     * We are currently NOT processing any chunk whatsoever.
     */
    NULL,

    /**
     * We have just finished processing the START of a chunk.
     */
    BEGINNING,

    /**
     * We are in the middle of processing a chunk.
     */
    MIDDLE,

    /**
     * We have just finished processing a non-chunk, which includes things like punctuation, etc.
     */
    NON_CHUNK

}
