package command_interface;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class CommandInterface {
    LexicalizedParser lexicalizedParser;
    Document tokens;

    public CommandInterface() throws Exception {
        lexicalizedParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        tokens = builder.parse(new File("src/zfiles/tokens.xml"));
    }

    public void readQuery(String input) {

    }
}
