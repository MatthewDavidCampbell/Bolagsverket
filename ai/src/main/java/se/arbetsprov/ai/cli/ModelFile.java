package se.arbetsprov.ai.cli;

import java.net.URI;
import java.util.List;

/**
 * A file the embedding model needs, pinned to an exact Hugging Face commit and checksum.
 *
 * @param name   file name in the cache folder
 * @param url    where --setup downloads it from
 * @param sha256 expected SHA-256, lower-case hex
 * @param size   expected size in bytes, shown before downloading
 */
public record ModelFile(String name, URI url, String sha256, long size) {

    public static final String MODEL = "model_quantized.onnx";
    public static final String TOKENIZER = "tokenizer.json";

    /** multilingual-e5-small: int8 model from Xenova (runs on any CPU), tokenizer from intfloat (MIT). */
    public static List<ModelFile> defaults() {
        return List.of(
                new ModelFile(MODEL, URI.create("https://huggingface.co/Xenova/multilingual-e5-small/resolve/"
                        + "761b726dd34fb83930e26aab4e9ac3899aa1fa78/onnx/model_quantized.onnx"),
                        "f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193", 118_308_185L),
                new ModelFile(TOKENIZER, URI.create("https://huggingface.co/intfloat/multilingual-e5-small/resolve/"
                        + "614241f622f53c4eeff9890bdc4f31cfecc418b3/onnx/tokenizer.json"),
                        "0b44a9d7b51c3c62626640cda0e2c2f70fdacdc25bbbd68038369d14ebdf4c39", 17_082_730L));
    }
}
