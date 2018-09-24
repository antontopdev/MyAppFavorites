package anton.top.dev.favorites;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;

import javax.inject.Inject;

import anton.top.dev.api.response.Facility;
import anton.top.dev.api.response.Favorite;
import anton.top.dev.app.Application;
import anton.top.dev.utils.FacilityStorage;
import anton.top.dev.utils.FavoriteStorage;
import anton.top.dev.utils.LocateStorage;
import anton.top.dev.utils.TokenStorage;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/*
 * Created by Anton Popov.
 */
@InjectViewState
public class FavoritePresenter extends MvpPresenter<FavoriteView>
        implements FavoriteStorage.OnFavoritesDownloadListener,
        LocateStorage.MapEventListener{

    @Inject
    TokenStorage tokenStorage;
    @Inject
    FavoriteStorage favoriteStorage;
    @Inject
    FacilityStorage facilityStorage;
    @Inject
    LocateStorage locateStorage;

    private boolean isFavoriteProcess = false;
    private boolean isUrlProcess = false;
    private boolean noNeedUpdateUrl = false;

    public FavoritePresenter() {
        Application.getComponent().inject(this);
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        favoriteStorage.downloadFavorites(tokenStorage.getToken(), this);
    }

    @Override
    public void attachView(FavoriteView view) {
        super.attachView(view);
        if(!noNeedUpdateUrl) {
            requestFavorites();
        } else {
            noNeedUpdateUrl = false;
        }
    }

    public void disableUpdateUrl() {
        noNeedUpdateUrl = true;
    }

    @Override
    public void onDownloadError(String message) {
        isUrlProcess = false;
        getViewState().showToast(this.getClass().getSimpleName() + " " + message);
    }

    public void requestFavorites() {
        updateFavoritesUrl();
        getViewState().showFavorites(favoriteStorage.getFavorites());
    }

    public void deleteFavoriteFacility(Favorite favorite, int itemId) {
        if (!isFavoriteProcess) {
            isFavoriteProcess = true;
            getViewState().removeFavoriteItem(itemId);
            if(favorite != null) {
                favoriteStorage.removeFacility(tokenStorage.getToken(), favorite)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                response -> {
                                    favoriteStorage.addRemovesId(favorite.facility.id);
                                    favoriteStorage.removeFavorites(favorite);
                                    getViewState().removeItem(itemId);
                                    isFavoriteProcess = false;
                                },
                                this::processingFavoriteError
                        );
            }
        }
    }

    private void updateFavoritesUrl() {
        isUrlProcess = true;
        favoriteStorage.checkFavoritesUrl(tokenStorage.getToken(), this);
    }

    public String getFavoriteUrl() {
        return favoriteStorage.getFacilityUrl();
    }

    public String getUserFirstName() {
        return tokenStorage.getUserFirstName();
    }

    private void processingFavoriteError(Throwable throwable) {
        isFavoriteProcess = false;
        getViewState().showToast("Favorites: " + throwable.getMessage());
    }

    @Override
    public void onUrlUpdated() {
        isUrlProcess = false;
    }

    public boolean urlReady() {
        return !isUrlProcess;
    }

    public void openFacility(Facility facility) {
        facilityStorage.setFacility(facility);
    }

    public void getLatLon(Facility facility) {
        locateStorage.getCoordinates(this, facility.address.state + "," + facility.address.city + "," + facility.address.addressOne + "," + facility.address.zip);
    }

    @Override
    public void openMap(String location) {
        getViewState().openMap(location);
    }

    @Override
    public void openMapError() {
        getViewState().showToast("Cannot open map.");
    }
}
