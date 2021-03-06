package net.milanaleksic.baobab.editor.model;

import net.milanaleksic.baobab.model.*;

/**
 * User: Milan Aleksic
 * Date: 1/24/13
 * Time: 3:46 PM
 */
public class FindDialogModel {

    private String searchText;

    @TransformerIgnoredProperty
    private boolean accepted;

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @TransformerFireUpdate
    public void copyFrom(FindDialogModel oldModel) {
        if (oldModel == null)
            return;
        this.searchText = oldModel.searchText;
    }
}
