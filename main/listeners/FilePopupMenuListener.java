package main.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import main.ComplierState;
import main.utility.FileUtil;
import main.utility.UiUtil;

public class FilePopupMenuListener implements ActionListener {
    UiUtil uiUtil;

    public FilePopupMenuListener(UiUtil uiUtil) {
        this.uiUtil = uiUtil;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command == "open") {
			FileUtil.open(ComplierState.selectedFile);
            
        } else if (command == "rename") {
			uiUtil.renameFile();

        } else if (command == "delete") {
            uiUtil.deleteFile(ComplierState.selectedFile);
        }
    }
}
