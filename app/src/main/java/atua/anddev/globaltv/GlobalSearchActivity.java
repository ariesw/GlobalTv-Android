package atua.anddev.globaltv;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import java.io.IOException;

import atua.anddev.globaltv.adapters.ChannelHolderAdapter;
import atua.anddev.globaltv.entity.Channel;

public class GlobalSearchActivity extends MainActivity implements GlobalServices, ChannelHolderAdapter.OnItemClickListener {
    private ProgressDialog progress;
    private String searchString;
    private ChannelHolderAdapter mAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set sub.xml as user interface layout
        setContentView(R.layout.globalsearch);

        getData();
        if (searchService.sizeOfSearchList() == 0)
            runProgressBar();
        else
            showSearchResults();
    }

    @Override
    public void onItemClick(Channel item, int viewId) {
        switch (viewId) {
            case R.id.favoriteIcon:
                changeFavorite(item);
                break;
            case R.id.title:
                guideActivity(item.getName());
                break;
            default:
                setTick(item);
                channelService.openURL(item.getUrl(), GlobalSearchActivity.this);
                break;
        }
    }

    private void getData() {
        Intent intent = getIntent();
        searchString = intent.getStringExtra("search");
    }

    private void runProgressBar() {
        progress = new ProgressDialog(this);
        progress.setMessage(getResources().getString(R.string.searching));
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(playlistService.sizeOfActivePlaylist());
        progress.setProgress(0);
        progress.show();

        Thread t = new Thread() {
            @Override
            public void run() {
                prepare_globalSearch();
                progress.dismiss();
                runOnUiThread(new Runnable() {
                    public void run() {
                        showSearchResults();
                    }
                });
            }
        };
        t.start();
    }

    private void prepare_globalSearch() {
        for (int i = 0; i < playlistService.sizeOfActivePlaylist(); i++) {
            progress.setProgress(i);
            playlistService.readPlaylist(i);

            String chName;
            for (Channel chn : channelService.getAllChannels()) {
                chName = chn.getName().toLowerCase();
                if (chName.contains(searchString.toLowerCase())) {
                    chn.setProvider(playlistService.getActivePlaylistById(i).getName());
                    searchService.addToSearchList(chn);
                }
            }
        }

    }

    public void showSearchResults() {
        TextView textView = (TextView) findViewById(R.id.globalsearchTextView1);
        textView.setText(getResources().getString(R.string.resultsfor) + " '" + searchString + "' - " +
                searchService.sizeOfSearchList() + " " + getResources().getString(R.string.channels));

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        mAdapter = new ChannelHolderAdapter(this, R.layout.item_channellist, searchService.getSearchList(), true);
        mAdapter.setOnItemClickListener(GlobalSearchActivity.this);
        recyclerView.setAdapter(mAdapter);
    }

    private void setTick(Channel channel) {
        mAdapter.setSelected(channel);
        mAdapter.notifyDataSetChanged();
    }

    private void changeFavorite(Channel item) {
        Boolean changesAllowed = true;
        for (int i = 0; i < favoriteService.sizeOfFavoriteList(); i++) {
            if (item.getName().equals(favoriteService.getFavoriteById(i).getName())
                    && item.getProvider().equals(favoriteService.getFavoriteById(i).getProv()))
                changesAllowed = false;
        }
        if (changesAllowed)
            favoriteService.addToFavoriteList(item.getName(), item.getProvider());
        else
            favoriteService.deleteFromFavoritesById(favoriteService.indexNameForFavorite(item.getName()));
        try {
            favoriteService.saveFavorites(GlobalSearchActivity.this);
        } catch (IOException ignored) {
        }
        mAdapter.notifyDataSetChanged();
    }

    private void guideActivity(String chName) {
        Intent intent = new Intent(this, GuideActivity.class);
        intent.putExtra("name", chName);
        startActivity(intent);
    }
}
