package se.arbetsprov.ai.index;

/**
 * How operations are searched by meaning.
 */
public enum IndexKind {

    /**
     * Exact search: the question is compared with every entry. Right for small catalogs (up to
     * about ten thousand entries); here it is six dot products.
     */
    FLAT,

    /**
     * Inverted file with product quantization, as in FAISS ({@code IVF…,PQ…}). Documented, not
     * built: it clusters vectors with k-means and compresses them with learned codebooks, which
     * needs thousands of training vectors. For a handful of operations it would be slower and
     * less accurate than {@link #FLAT}; it pays off at millions of entries.
     */
    IVF_PQ
}
