package org.sana.android.net.commands;

/**
 * Created by Albert on 9/3/2016.
 */
public interface ICommand {
    public void cancel();
    public void execute() ;
    public void redo();
}
