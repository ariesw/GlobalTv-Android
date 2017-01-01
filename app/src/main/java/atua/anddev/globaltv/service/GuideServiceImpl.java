package atua.anddev.globaltv.service;

import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import atua.anddev.globaltv.Global;
import atua.anddev.globaltv.MainActivity;
import atua.anddev.globaltv.entity.ChannelGuide;
import atua.anddev.globaltv.entity.GuideProv;
import atua.anddev.globaltv.entity.Programme;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class GuideServiceImpl implements GuideService {
    private static final DateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss Z");
    private String myPath = MainActivity.myPath;
    private Calendar currentTime;

    public boolean checkForUpdate(Context context) {
        boolean result = false;
        if (!isGuideExist()) {
            result = true;
        } else {
            currentTime = Calendar.getInstance();
            parseGuide(context);
            result = checkGuideDates();
        }
        return result;
    }

    private boolean isGuideExist() {
        boolean result;
        GuideProv guideProv = guideProvList.get(Global.selectedGuideProv);
        File file = new File(myPath + "/" +guideProv.getFile());
        result = file.exists();
        return result;
    }

    public void parseGuide(Context context) {
        Log.d("globaltv", "parsing program guide...");
        String dispName = null, id = null, start = null, stop = null, channel = null;
        String endTag, text = null, title = null, desc = null, category = null;
        Global.guideLoaded = false;
        channelGuideList.clear();
        programmeList.clear();
        try {
            XmlPullParser xpp;
            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            xppf.setNamespaceAware(true);
            xpp = xppf.newPullParser();
            GuideProv guideProv = guideProvList.get(Global.selectedGuideProv);
            FileInputStream fis = context.getApplicationContext().openFileInput(guideProv.getFile());
            GZIPInputStream gzipIs = new GZIPInputStream(fis);
            xpp.setInput(gzipIs, null);

            String tmp = "";
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            tmp = xpp.getAttributeName(i);
                            if (tmp.equals("id"))
                                id = xpp.getAttributeValue(i);
                            if (tmp.equals("start"))
                                start = xpp.getAttributeValue(i);
                            if (tmp.equals("stop"))
                                stop = xpp.getAttributeValue(i);
                            if (tmp.equals("channel"))
                                channel = xpp.getAttributeValue(i);
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        endTag = xpp.getName();
                        if (endTag.equals("display-name"))
                            dispName = text;
                        if (endTag.equals("title"))
                            title = text;
                        if (endTag.equals("desc"))
                            desc = text;
                        if (endTag.equals("category"))
                            category = text;

                        if (endTag.equals("channel")) {
                            ChannelGuide channelGuide = new ChannelGuide(id, null, dispName);
                            channelGuideList.add(channelGuide);
                            id = null;
                            dispName = null;
                        }
                        if (endTag.equals("programme")) {
                            Programme programme = new Programme(start, stop, channel, title, desc, category);
                            programmeList.add(programme);
                            start = null;
                            stop = null;
                            channel = null;
                            title = null;
                            desc = null;
                            category = null;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        text = xpp.getText();
                        break;

                    default:
                        break;
                }
                xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
            Log.d("globaltv", "parsing error "+e.getMessage());
        }
        Log.d("globaltv", "parsing finished");
        Global.guideLoaded = true;
    }

    @Override
    public String getProgramTitle(String chName) {
        currentTime = Calendar.getInstance();
        String id = getIdByChannelName(chName);
        return getProgramTitlebyId(id);
    }

    @Override
    public String getProgramDesc(String chName) {
        currentTime = Calendar.getInstance();
        String id = getIdByChannelName(chName);
        return getProgramDescbyId(id);
    }

    private boolean checkGuideDates() {
        boolean result = false;
        List<String> dateList = new ArrayList<String>();
        if (channelGuideList.size() > 0) {
            String chId = channelGuideList.get(0).getId();
            for (Programme programme : programmeList) {
                if (programme.getChannel().equals(chId)) {
                    dateList.add(programme.getStart());
                }
            }
            String startDateStr = dateList.get(0);
            String endDateStr = dateList.get(dateList.size() - 1);
            Calendar startDate = Calendar.getInstance();
            Calendar endDate = Calendar.getInstance();
            try {
                startDate.setTime(sdf.parse(startDateStr));
                endDate.setTime(sdf.parse(endDateStr));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            result = !(currentTime.after(startDate) && currentTime.before(endDate));
        }
        return result;
    }

    private String getIdByChannelName(String chName) {
        String result = null;
        for (ChannelGuide channelGuide : channelGuideList) {
            if (chName.equals(channelGuide.getDisplayName()))
                result = channelGuide.getId();
        }
        return result;
    }

    private String getProgramTitlebyId(String id) {
        String result = null;
        for (Programme programme : programmeList) {
            if (programme.getChannel().equals(id)) {
                String startDateStr = programme.getStart();
                String endDateStr = programme.getStop();
                Calendar startDate = Calendar.getInstance();
                Calendar endDate = Calendar.getInstance();

                try {
                    startDate.setTime(sdf.parse(startDateStr));
                    endDate.setTime(sdf.parse(endDateStr));
                } catch (ParseException e) {
                    System.out.println(endDateStr);
                    e.printStackTrace();
                }
                if (currentTime.after(startDate) && currentTime.before(endDate)) {
                    result = programme.getTitle();
                }
            }
        }
        return result;
    }

    private String getProgramDescbyId(String id) {
        String result = null;
        for (Programme programme : programmeList) {
            if (programme.getChannel().equals(id)) {
                String startDateStr = programme.getStart();
                String endDateStr = programme.getStop();
                Calendar startDate = Calendar.getInstance();
                Calendar endDate = Calendar.getInstance();
                try {
                    if (!startDateStr.isEmpty())
                        startDate.setTime(sdf.parse(startDateStr));
                    if (!endDateStr.isEmpty())
                        endDate.setTime(sdf.parse(endDateStr));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (currentTime.after(startDate) && currentTime.before(endDate)) {
                    result = programme.getDesc();
                }
            }
        }
        return result;
    }

    public List<Programme> getChannelGuide(String chName) {
        List<Programme> result = new ArrayList<Programme>();
        String id = getIdByChannelName(chName);
        for (Programme programme : programmeList) {
            if (programme.getChannel().equals(id)) {
                result.add(programme);
            }
        }
        return result;
    }

    public void setupGuideProvList() {
        guideProvList.add(new GuideProv("Epg.in.ua", "http://epg.in.ua/epg/tvprogram_ua_ru.gz",
                "epginua.xml.gz"));
        guideProvList.add(new GuideProv("TeleGuide.info", "http://www.teleguide.info/download/new3/xmltv.xml.gz",
                "teleguide.xml.gz"));
        guideProvList.add(new GuideProv("Torrent-tv.ru", "http://api.torrent-tv.ru/ttv.xmltv.xml.gz",
                "ttvru.xml.gz"));
    }

    public String getTotalTimePeriod() {
        String result = null;
        final DateFormat totalSdf = new SimpleDateFormat("dd.MM");
        List<String> dateList = new ArrayList<String>();
        if (channelGuideList.size() > 0) {
            String chId = channelGuideList.get(0).getId();
            for (Programme programme : programmeList) {
                if (programme.getChannel().equals(chId)) {
                    dateList.add(programme.getStart());
                }
            }
            String startDateStr = dateList.get(0);
            String endDateStr = dateList.get(dateList.size() - 1);
            Calendar startDate = Calendar.getInstance();
            Calendar endDate = Calendar.getInstance();
            try {
                startDate.setTime(sdf.parse(startDateStr));
                endDate.setTime(sdf.parse(endDateStr));
                result = totalSdf.format(startDate.getTime()) + " - " + totalSdf.format(endDate.getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}