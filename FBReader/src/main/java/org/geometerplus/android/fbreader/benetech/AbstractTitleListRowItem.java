package org.geometerplus.android.fbreader.benetech;

import android.util.Log;

import org.geometerplus.fbreader.library.Book;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;

import java.util.Date;

/**
 * Created by animal@martus.org on 5/2/16.
 */
abstract public class AbstractTitleListRowItem implements Comparable<AbstractTitleListRowItem> {

    public int compareTo(AbstractTitleListRowItem another) {
        String thisTitle = getBookTitle();
        String otherTitle = another.getBookTitle();
        if (thisTitle == otherTitle)
            return 0;

        if (thisTitle == null && otherTitle == null)
            return 0;

        if (thisTitle == null)
            return -1;

        if (otherTitle == null)
            return 1;

        return thisTitle.compareTo(otherTitle);
    }

    abstract public String getBookFilePath();

    abstract public ZLFile getBookZlFile();

    abstract public Book getBook();

    abstract public String getBookTitle();

    abstract public String getAuthors();

    abstract public boolean isDownloadedBook();

    abstract public long getBookId();

    abstract public Date getCompareDate();

    abstract public boolean canDeleteFromReadinglist();

    @Override
    public boolean equals(Object o) {
        if(o instanceof  AbstractTitleListRowItem){
            if(((AbstractTitleListRowItem) o).getBookTitle().contains("Hallows")
                    && this.getBookTitle().contains("Hallows")){
                Log.d("hallows", "hallows");
            }
            return(((AbstractTitleListRowItem) o).getBookId() == this.getBookId());
        }
        else return false;
    }
}
