package anaconda.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import anaconda.Anaconda;
import anaconda.testutil.JavaFxTestSupport;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Tests command submission through the actual window with isolated task storage.
 */
public class MainWindowTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    public static void setUpToolkit() throws InterruptedException {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    public void setAnaconda_submitCommands_rendersDistinctRowsAndClearsInput() throws Exception {
        JavaFxTestSupport.runOnFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(MainWindow.class.getResource("/View/MainWindow.fxml"));
            AnchorPane root = loader.load();
            loader.<MainWindow>getController().setAnaconda(new Anaconda(temporaryDirectory.resolve("tasks.txt")));
            new Scene(root);
            TextField input = (TextField) root.lookup("#userInput");
            Button send = (Button) root.lookup("#sendButton");
            ScrollPane scrollPane = (ScrollPane) root.lookup("#scrollPane");
            VBox dialogs = (VBox) scrollPane.getContent();
            assertTrue(scrollPane.vvalueProperty().isBound());

            for (String command : new String[] {"todo read book", "todo", "nonsense", "bye"}) {
                input.setText(command);
                send.fire();
                assertEquals("", input.getText());
            }

            root.applyCss();
            root.layout();
            String[] expectedStyles = {"success", "warning", "error", "success"};
            String[] expectedColors = {"#174d35", "#ffe082", "#b3261e", "#174d35"};
            String[] expectedReplyColors = {"#d9ffe2", "#fff3c4", "#ffe4e1", "#d9ffe2"};
            assertEquals(8, dialogs.getChildren().size());
            for (int i = 0; i < dialogs.getChildren().size(); i += 2) {
                DialogBox command = (DialogBox) dialogs.getChildren().get(i);
                assertFalse(command.getChildren().stream().anyMatch(ImageView.class::isInstance));
                DialogBox reply = (DialogBox) dialogs.getChildren().get(i + 1);
                assertTrue(reply.getChildren().get(0) instanceof ImageView);
                assertTrue(command.getStyleClass().contains(expectedStyles[i / 2]));
                assertTrue(reply.getStyleClass().contains(expectedStyles[i / 2]));
                assertEquals(Color.web(expectedColors[i / 2]), command.getBackground().getFills().get(0).getFill());
                Label replyText = (Label) reply.getChildren().get(1);
                assertEquals(Color.web(expectedReplyColors[i / 2]),
                        replyText.getBackground().getFills().get(0).getFill());
            }
            DialogBox added = (DialogBox) dialogs.getChildren().get(1);
            assertTrue(((Label) added.getChildren().get(1)).getText().contains("read book"));
            DialogBox warning = (DialogBox) dialogs.getChildren().get(3);
            assertTrue(((Label) warning.getChildren().get(1)).getText().contains("cannot be empty"));
            DialogBox error = (DialogBox) dialogs.getChildren().get(5);
            assertTrue(((Label) error.getChildren().get(1)).getText().contains("Oops!"));
            DialogBox goodbye = (DialogBox) dialogs.getChildren().get(7);
            assertEquals(0.5, goodbye.getChildren().get(0).getOpacity());
            return null;
        });
    }
}
