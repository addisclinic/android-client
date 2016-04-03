package org.moca.net.commands;

import java.util.List;

/**
 * Created by Albert on 4/3/2016.
 */
public interface ICommand {
    public void execute() ;
    public void redo();
    public List<?> getResultList();
}
