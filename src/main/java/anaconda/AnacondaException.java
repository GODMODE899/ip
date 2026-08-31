package anaconda;

/**
 * Represents an invalid command or argument supplied to Anaconda.
 */
public class AnacondaException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception containing a user-facing explanation.
     *
     * @param message Explanation of the invalid input.
     */
    public AnacondaException(String message) {
        super(message);
    }
}
