package org.sana.android.testHarness;

import org.sana.android.net.clients.NetworkClient;
import org.sana.android.net.commands.ICommand;
import org.sana.android.net.commands.LoginCommand;

/**
 * Created by Albert on 9/4/2016.
 */
public class TestHarnessClient extends NetworkClient {

    public TestHarnessClient() {
        super();
    }

    public void login() {
        ICommand command = new LoginCommand(null, "guest", "Sanamobile1");
        executeCommand(command);
    }

    public static TestHarnessClient getInstance() {
        return new TestHarnessClient();
    }
}
