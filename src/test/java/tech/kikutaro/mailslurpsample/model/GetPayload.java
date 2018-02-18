package tech.kikutaro.mailslurpsample.model;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author kikuta
 */
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetPayload implements Serializable {
    private String body;
    private String from;
    private String id;
    private String received;
    private String returnPath;
    private String subject;
    private List<String> to;
}
