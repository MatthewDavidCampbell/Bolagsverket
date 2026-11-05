package se.arbetsprov.ai.catalog;

import java.util.List;

/**
 * One thing the chat can do, written once in canonical English. The instruction and description
 * are what the question is matched against; the method, path template and parameters are all
 * a request can ever be built from.
 *
 * @param id           stable name, for example "countries.list"
 * @param instruction  short English instruction, for example "List countries"
 * @param description  one English sentence on what it does and which values it takes
 * @param method       HTTP method
 * @param pathTemplate path with {@code {name}} placeholders for path parameters
 * @param parameters   allowed parameters
 * @param needsUser    whether the call carries X-User-Id
 * @param examples     short English example requests; the description alone does not separate
 *                     operations well enough, so the index also holds one vector per example
 */
public record Operation(
        String id,
        String instruction,
        String description,
        String method,
        String pathTemplate,
        List<ParameterSpec> parameters,
        boolean needsUser,
        List<String> examples) {

    public Operation {
        parameters = List.copyOf(parameters);
        examples = List.copyOf(examples);
    }

    /** The text that is embedded for the index: instruction and description together. */
    public String passage() {
        return instruction + ". " + description;
    }
}
