package own.projects.lemiroapp;

import android.os.Parcel;
import android.os.Parcelable;

public class Options implements Parcelable {

	protected MillVariant millVariant;
	protected static enum MillVariant {
		MILL5, MILL7, MILL9
	};

    protected Player playerWhite;
	protected Player playerBlack;
	protected static enum Difficulties {
		EASY, NORMAL, HARD, HARDER, HARDEST
	};

	protected Color whoStarts;
	protected static enum Color {
		WHITE, BLACK, RED, GREEN, NOTHING, INVALID
	};
	
	public Options() {
		this.millVariant = null;
		this.whoStarts = null;
		this.playerWhite = new Player(Options.Color.WHITE);
		this.playerBlack = new Player(Options.Color.BLACK);;
	}
	
	public Options(Parcel in) {
		Options o = in.readParcelable(getClass().getClassLoader());
		this.millVariant = o.millVariant;
        this.whoStarts = o.whoStarts;
        this.playerWhite = o.playerWhite;
		this.playerBlack = o.playerBlack;
	}

    @Override
    public String toString() {
        return "Options{" +
                "millVariant=" + millVariant +
                ", playerWhite=" + playerWhite +
                ", playerBlack=" + playerBlack +
                ", whoStarts=" + whoStarts +
                '}';
    }

    /* everything below here is for implementing Parcelable */

    //TODO make sure old values are restored on restart of activity

    // 99.9% of the time you can just ignore this
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
	public void writeToParcel(Parcel dest, int flags) {
    	dest.writeValue(millVariant);
    	dest.writeValue(whoStarts);
    	dest.writeValue(playerWhite);
    	dest.writeValue(playerBlack);
	}

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Options> CREATOR = new Parcelable.Creator<Options>() {
        public Options createFromParcel(Parcel source) {
        	final Options o = new Options();
            o.millVariant = (MillVariant) source.readValue(Options.class.getClassLoader());
            o.whoStarts = (Color) source.readValue(Options.class.getClassLoader());
            o.playerWhite = (Player) source.readValue(Options.class.getClassLoader());
            o.playerBlack = (Player) source.readValue(Options.class.getClassLoader());
            return o;
        }

        public Options[] newArray(int size) {
        	throw new UnsupportedOperationException();
        }
    };
    
}
