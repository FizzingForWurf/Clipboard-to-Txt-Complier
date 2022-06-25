package main.utility;

import java.io.File;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import main.ComplierState;
import main.MainWindow;

public class UiUtil {
    MainWindow mainWindow;

    public UiUtil(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    /**
     * Update cursor position to start of last line to show textArea contents
     * 
     * @param textArea
     */
    public static void bringCursorToStart(JTextArea textArea) {
        int lastLineNumber = textArea.getLineCount() - 1;
        try {
            int lineStartCaretPosition = textArea.getLineStartOffset(lastLineNumber);
            textArea.setCaretPosition(lineStartCaretPosition);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called after file is saved.
     * Increment number in <code>numberTextField</code>
     */
    public void incrementNumberTextField() {
        if (!mainWindow.numberTextField.getText().isEmpty()) {
            try {
                int number = Integer.valueOf(mainWindow.numberTextField.getText());
                number++;
                mainWindow.numberTextField.setText(Integer.toString(number));

            } catch (NumberFormatException error) {
                error.printStackTrace();
                MainWindow.consoleLog("Error incrementing file number");
            }
        }
    }

    /**
     * Gets file number and pad with leading zeros and combine with leading
     * and trailing to show file name in <code>fileNameTextField</code>
     */
    public void updateFileNameTextField() {
        String number = mainWindow.numberTextField.getText().trim();
        String limit = mainWindow.leadingZerosTextField.getText().trim();

        boolean isNumberEmpty = number.isEmpty();
        boolean isLimitEmpty = limit.equals("");
        boolean isLimitZero = limit.equals("0");

        if (!isNumberEmpty && !isLimitEmpty && !isLimitZero) {
            try {
                int limitInt = Integer.valueOf(limit);
                int numberInt = Integer.valueOf(number);
                limit = String.valueOf(limitInt);

                String newFormat = "%0" + limit + "d";
                number = String.format(newFormat, numberInt);

            } catch (NumberFormatException e) {
                e.printStackTrace();
                MainWindow.consoleLog("Error updating file name!");
            }
        }

        String leading = mainWindow.leadingTextField.getText();
        String trailing = mainWindow.trailingTextField.getText();
        if (number.isEmpty()) {
            mainWindow.fileNameTextField.setText(leading + " " + trailing);
        } else {
            mainWindow.fileNameTextField.setText(leading + " " + number + " " + trailing);
        }
    }

    /**
     * Update file browser with file directory from
     * <code>outputFolderTextField</code>. Checks if directory path
     * exists before executing refresh and updating state file variables
     */
    public void updateFileBrowser() {
        String tempPath = mainWindow.outputFolderTextField.getText();
        File tempFile = new File(tempPath);

        if (tempFile.exists() && tempFile.isDirectory()) {
            ComplierState.selectedFile = null;
            ComplierState.currentDirectory = tempFile;
            ComplierState.parentDirectory = tempFile.getParentFile();

            mainWindow.fileBrowser.buildTreeFromPath(tempPath);
        }
    }

    /**
	 * Display file contents when text (.txt) FILE is clicked in 
     * fileBrowser. Update <code>selectedFile</code> state.
     * 
	 * @param selectedFile File object of selected file
	 */
	public void handleFileBrowserFileClick(File selectedFile) {
		// Update selectedFile state
		ComplierState.selectedFile = selectedFile;

		// Check if text file selected and show contents
		String content = FileUtil.readTextFile(selectedFile);
		mainWindow.fileViewerTextArea.setText(content);
		mainWindow.fileViewerTextArea.setCaretPosition(0);
	}

    /**
	 * Updates <code>outputFolderTextField</code> which will update 
     * file browser when FOLDER is clicked in <code>fileBrowser</code>. 
     * Reset <code>fileViewerTextArea</code> to empty.
     * 
	 * @param path String path of selected folder
	 */
	public void handleFileBrowserFolderClick(String path) {
		// Update new folder in fileBrowser
		mainWindow.outputFolderTextField.setText(path);

		// Reset fileViewer to empty as folder is selected
		mainWindow.fileViewerTextArea.setText("");
	}

    private void handleClipboardUpdate(String clipboardText, boolean checkAutoSave) {
        // Do not handle clipboard changes if NOT tracking
        if (!ComplierState.isTracking)
            return;

        ComplierState.previousClipboard = clipboardText;

        String currentText = mainWindow.currentFileTextArea.getText();

        if (currentText.isEmpty())
            currentText += clipboardText;
        else
            currentText += "\n" + clipboardText;

        mainWindow.currentFileTextArea.setText(currentText);
        mainWindow.clipboardTextArea.setText(clipboardText.trim());

        bringCursorToStart(mainWindow.currentFileTextArea);
        bringCursorToStart(mainWindow.clipboardTextArea);

        // Save file if clipboard has multiple lines
        if (checkAutoSave && ComplierState.multiLineAutosave) {
            if (clipboardText.contains("\n"))
                saveFile();
        }
    }

    public void handleClipboardUpdate(String clipboardText) {
        handleClipboardUpdate(clipboardText, true);
    }

    public void duplicateClipboard() {
        String previousClipboard = mainWindow.clipboardTextArea.getText();

        if (previousClipboard.trim().isEmpty()) {
            System.out.println("Previous clipboard is empty!");
            MainWindow.consoleLog("Previous clipboard is empty!");
        } else
            // Do not check autosave, duplicate previous clipboard even with newline
            handleClipboardUpdate(ComplierState.previousClipboard, false);
    }

    /**
     * Solicits new file name from user via InputDialog. Update
     * <code>fileBrowser</code> only file renamed successfully to
     * show newly updated file name.
     * 
     * <p>
     * Only allows text (.txt) files to be renamed.
     * </p>
     */
    public void renameFile() {
        File file = ComplierState.selectedFile;

        if (file == null || !file.exists()) {
            System.out.println("Please select a file to rename!");
            MainWindow.consoleLog("Please select a file to rename!");
            return;
        }

        if (!FileUtil.isTxtFile(file)) {
            System.out.println("Only allowed to rename text (.txt) files!");
            MainWindow.consoleLog("Only allowed to rename text (.txt) files!");
            return;
        }

        String renamedFileName = DialogUtil.showRenameDialog(mainWindow, file);
        // User cancel, empty file name or same file name
        if (renamedFileName == null)
            return;

        boolean renameSuccess = FileUtil.rename(file, renamedFileName);
        if (renameSuccess)
            updateFileBrowser();
    }
    
    /**
     * Delete user selected file. Ask user to confirm delete operation.
     * Update <code>fileBrowser</code> to show file removed from tree.
     * 
     * <p>
     * Only allows text (.txt) files to be deleted.
     * </p>
     * 
     * @param selectedFile File to be deleted
     */
    public void deleteFile(File selectedFile) {
        if (!FileUtil.isTxtFile(selectedFile)) {
            System.out.println("Only allowed to delete text (.txt) files!");
            MainWindow.consoleLog("Only allowed to delete text (.txt) files!");
            return;
        }

        boolean confirmDelete = DialogUtil.showConfirmDeleteDialog(mainWindow);

        if (confirmDelete) {
            if (selectedFile.delete()) {
                System.out.println("Successfully deleted: " + selectedFile.getAbsolutePath());
                MainWindow.consoleLog("Successfully deleted: " + selectedFile.getAbsolutePath());

                // Update file browser to remove deleted file
                updateFileBrowser();
            } else {
                System.out.println("Error occurred deleting: " + selectedFile.getAbsolutePath());
                MainWindow.consoleLog("Error occurred deleting: " + selectedFile.getAbsolutePath());
            }
        }
    }

    /**
     * <p>
     * Save text in <code>currentFileTextArea</code> to valid current directory
     * and under user input file name. If file name already exists in current
     * directory, confirm with user to override file or abort save operation.
     * </p>
     * 
     * <p>
     * Once file is saved successfully, reset <code>currentFileTextArea</code>
     * to empty, refresh file browser to show newly added file and increment
     * <code>numberTextField</code>.
     * </p>
     */
    public void saveFile() {
        String fileDirectory = mainWindow.outputFolderTextField.getText();
        String fileName = mainWindow.fileNameTextField.getText().trim();
        String currentText = mainWindow.currentFileTextArea.getText();

        // Don't save if directory or file name is empty
        if (fileDirectory.isEmpty() || fileName.isEmpty()) {
            System.out.println("Error! Empty file name or directory. Please enter name or folder");
            MainWindow.consoleLog("Error! Empty file name or directory. Please enter name or folder");
            return;
        }

        String filePathString = fileDirectory + "\\" + fileName + ".txt";
        File tempFile = new File(filePathString);

        // Ask user to override if there is an existing file
        if (tempFile.exists()) {
            boolean confirmSave = DialogUtil.showConfirmSaveDialog(mainWindow);

            // User chooses NOT to override file, abort saving file
            if (!confirmSave) {
                System.out.println("Abort overriding existing file!");
                MainWindow.consoleLog("Abort overriding existing file!");
                return;
            }
        }

        boolean saveSuccess = FileUtil.save(filePathString, currentText);
        if (saveSuccess) {
            // Reset current file text
            mainWindow.currentFileTextArea.setText("");

            // Update file browser to show new file
            updateFileBrowser();

            incrementNumberTextField();
        }
    }
}
