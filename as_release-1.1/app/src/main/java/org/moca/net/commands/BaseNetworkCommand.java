package org.moca.net.commands;

import java.util.List;

/**
 * Created by Albert on 4/3/2016.
 */
public abstract class BaseNetworkCommand implements ICommand  {
    private List<?> resultList;;

    public List<?> getResultList() {
        return resultList;
    }


    @Override
    public void redo()  {
        execute();
    }
}
