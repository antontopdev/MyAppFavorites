package anton.top.dev.favorites;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.arellomobile.mvp.MvpAppCompatFragment;
import com.arellomobile.mvp.presenter.InjectPresenter;

import java.util.List;

import anton.top.dev.api.response.Facility;
import anton.top.dev.api.response.Favorite;
import anton.top.dev.app.Application;
import anton.top.dev.app.facility.FacilityActivity;
import anton.top.dev.mvp.home.favorites.FavoriteView;
import anton.top.dev.utils.MixMap;
import anton.top.dev.utils.MixpanelHelper;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/*
 * Created by Anton Popov.
 */
public class FavoriteFragment extends MvpAppCompatFragment
        implements FavoriteView,
        FavoritesAdapter.OnFavoriteClickListener {

    @InjectPresenter
    FavoritePresenter presenter;

    @BindView(R.id.tv_no_favorites)
    protected TextView noFavorites;
    @BindView(R.id.rv_favorites)
    protected RecyclerView rvFavorites;
    @BindView(R.id.btn_share)
    protected TextView btnShare;

    @BindView(R.id.dialog_share_favorites)
    protected ConstraintLayout dialogShare;

    private FavoritesAdapter adapter;
    private boolean isShareDialog = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        setFavoritesListSettings();
    }

    private void setFavoritesListSettings() {
        adapter = new FavoritesAdapter(this);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(adapter);
    }

    public void updateFavoritesList() {
        new Handler().postDelayed(() -> presenter.requestFavorites(), 200);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void internetError() {
        showToast("No internet connection");
    }

    @Override
    public void showFavorites(List<Favorite> favorites) {
        if (favorites.size() == 0) {
            noFavorites.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
            btnShare.setVisibility(View.GONE);
        } else {
            noFavorites.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.VISIBLE);
            btnShare.setVisibility(View.VISIBLE);
            adapter.setItems(favorites);
        }
    }

    @Override
    public void onFacilityClick(Facility facility) {
        Application.getMixpanel().sendEvent(MixpanelHelper.TAP_ON_FACILITY, new MixMap(new String[]{"Facility", facility.name}));
        presenter.openFacility(facility);
        Intent facilityIntent = new Intent(getContext(), FacilityActivity.class);
        startActivity(facilityIntent);
    }

    @Override
    public void onFavoriteClick(Favorite favorite, int position) {
        Application.getMixpanel().sendEvent(MixpanelHelper.REMOVE_FAVORITE, new MixMap(new String[]{"Facility", favorite.facility.name}));
        presenter.deleteFavoriteFacility(favorite, position);
    }

    @Override
    public void onMapClick(Facility facility) {
        Application.getMixpanel().sendEvent(MixpanelHelper.MAP_ON_SEARCH, new MixMap(new String[]{"Facility", facility.name}));
        presenter.getLatLon(facility);
    }

    @Override
    public void openMap(String location) {
        String uri = "http://maps.google.com/maps?q=loc:" + location;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            try {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(unrestrictedIntent);
            } catch (ActivityNotFoundException innerEx) {
                showToast("Cannot open map");
            }
        }
    }

    @Override
    public void removeItem(int itemPosition) {
        adapter.removeItem(itemPosition);
    }

    @Override
    public void removeFavoriteItem(int itemPosition) {
        adapter.removeFavoriteItem(itemPosition);
    }

    @OnClick(R.id.btn_share)
    public void shareFavorites() {
        if (presenter.urlReady()) {
            showShareDialog();
        }
    }

    public void showShareDialog() {
        isShareDialog = true;
        Animation animation = new TranslateAnimation(0, 0, 1000, 0);
        animation.setDuration(300);
        dialogShare.startAnimation(animation);
        dialogShare.setVisibility(View.VISIBLE);
    }

    public void hideShareDialog() {
        if (isShareDialog) {
            Animation animation = new TranslateAnimation(0, 0, 0, 1000);
            animation.setDuration(300);
            dialogShare.startAnimation(animation);
            dialogShare.setVisibility(View.GONE);
            isShareDialog = false;
        }
    }

    @OnClick(R.id.dialog_back)
    protected void dialogHideClick() {
        hideShareDialog();
    }

    public boolean isShareDialog() {
        return isShareDialog;
    }

    @OnClick(R.id.btn_phone)
    protected void onSmsShare() {
        hideShareDialog();
        if (getActivity() != null) {
            presenter.disableUpdateUrl();
            ((MainActivity) getActivity()).onPhoneShare(presenter.getFavoriteUrl());
        }
    }

    @OnClick(R.id.btn_email)
    protected void onEmailShare() {
        hideShareDialog();
        presenter.disableUpdateUrl();
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/html");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT,presenter.getUserFirstName() + " " + getString(R.string.email_favorites_subject));
        emailIntent.putExtra(Intent.EXTRA_TEXT,
                Html.fromHtml(getString(R.string.email_favorites_title)
                        + presenter.getUserFirstName() + " "
                        + getString(R.string.email_favorites_text)
                        + " " + presenter.getFavoriteUrl() + " "
                        + getString(R.string.email_end)));
        startActivity(Intent.createChooser(emailIntent, "Email:"));
    }
}
