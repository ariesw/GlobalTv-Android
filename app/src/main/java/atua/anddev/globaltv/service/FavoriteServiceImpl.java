package atua.anddev.globaltv.service;


import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import atua.anddev.globaltv.GlobalServices;
import atua.anddev.globaltv.entity.Channel;

public class FavoriteServiceImpl implements FavoriteService, GlobalServices {

    @Override
    public int indexOfFavoriteByChannel(Channel channel) {
        int result = -1;
        for (int i = 0; i < sizeOfFavoriteList(); i++) {
            if (channel.getName().equals(favorites.get(i).getName()) &&
                    channel.getProvider().equals(favorites.get(i).getProvider())) {
                result = i;
            }
        }
        return result;
    }

    @Override
    public List<Channel> getFavoriteList() {
        return favorites;
    }

    @Override
    public void deleteFromFavoritesById(int id) {
        favorites.remove(id);
    }

    @Override
    public void deleteFromFavoritesByChannel(Channel channel) {
        for (int i = 0; i < sizeOfFavoriteList(); i++) {
            if (channel.getName().equals(getFavoriteById(i).getName()) &&
                    channel.getProvider().equals(getFavoriteById(i).getProvider())) {
                favorites.remove(i);
            }
        }
    }

    @Override
    public void addToFavoriteList(Channel channel) {
        favorites.add(channel);
    }

    @Override
    public Channel getFavoriteById(int id) {
        return favorites.get(id);
    }

    @Override
    public int sizeOfFavoriteList() {
        return favorites.size();
    }

    @Override
    public int indexNameForFavorite(String name) {
        int result = -1;
        for (int i = 0; i < favorites.size(); i++) {
            if (name.equals(favorites.get(i).getName()))
                result = i;
        }
        return result;
    }

    @Override
    public boolean isChannelFavorite(Channel item) {
        Boolean result = false;
        for (Channel fav : getFavoriteList()) {
            if (item.getName().equals(fav.getName())
                    && item.getProvider().equals(fav.getProvider()))
                result = true;
        }
        return result;
    }

    @Override
    public void saveFavorites(Context context) throws FileNotFoundException, IOException {
        FileOutputStream fos;
        fos = context.getApplicationContext().openFileOutput("favorites.xml", Context.MODE_PRIVATE);
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(fos, "UTF-8");
        serializer.startDocument(null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag(null, "root");

        for (int j = 0; j < sizeOfFavoriteList(); j++) {
            serializer.startTag(null, "favorites");

            serializer.startTag(null, "channel");
            serializer.text(getFavoriteById(j).getName());
            serializer.endTag(null, "channel");

            serializer.startTag(null, "playlist");
            serializer.text(getFavoriteById(j).getProvider());
            serializer.endTag(null, "playlist");

            serializer.endTag(null, "favorites");
        }
        serializer.endDocument();
        serializer.flush();
        fos.close();
    }

    @Override
    public void loadFavorites(Context context) throws IOException {
        String text = null, name = null, prov = null, endTag;
        try {
            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            xppf.setNamespaceAware(true);
            XmlPullParser xpp = xppf.newPullParser();
            FileInputStream fis = context.getApplicationContext().openFileInput("favorites.xml");
            xpp.setInput(fis, null);
            int type = xpp.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_DOCUMENT) {
                    // nothing to do
                } else if (type == XmlPullParser.START_TAG) {
                    // nothing to do
                } else if (type == XmlPullParser.END_TAG) {
                    endTag = xpp.getName();
                    if (endTag.equals("channel"))
                        name = text;
                    if (endTag.equals("playlist"))
                        prov = text;
                    if (endTag.equals("favorites")) {
                        addToFavoriteList(new Channel(name, null, null, null, prov));
                    }
                } else if (type == XmlPullParser.TEXT) {
                    text = xpp.getText();
                }
                type = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }
}
