package anaconda.gui;

import anaconda.Anaconda;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the main GUI.
 */
public class MainWindow extends AnchorPane {
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox dialogContainer;
    @FXML
    private TextField userInput;
    @FXML
    private Button sendButton;

    private Anaconda anaconda;

    private final Image userImage = new Image(this.getClass().getResourceAsStream("/images/DaUser.png"));
    private final Image anacondaImage = new Image(this.getClass().getResourceAsStream("/images/DaDuke.png"));

    /**
     * Keeps the conversation scrolled to the newest dialog.
     */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
    }

    /**
     * Supplies the Anaconda instance that processes user commands.
     *
     * @param anaconda Anaconda instance shared by this window.
     */
    public void setAnaconda(Anaconda anaconda) {
        this.anaconda = anaconda;
    }

    /**
     * Appends the user input and Anaconda's response to the dialog container, then clears the input field.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        String response = anaconda.getResponse(input);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getAnacondaDialog(response, anacondaImage)
        );
        userInput.clear();
    }
}
