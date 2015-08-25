package com.auriferous.atch;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.util.Base64;
import android.util.Log;

import com.auriferous.atch.Callbacks.FuncCallback;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.Users.User;
import com.auriferous.atch.Users.UserList;
import com.auriferous.atch.Users.UserListAdapter;
import com.facebook.FacebookSdk;
import com.google.android.gms.maps.MapsInitializer;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseRole;
import com.parse.ParseUser;

import java.security.MessageDigest;
import java.util.Date;
import java.util.List;

//laptop release qgG7TnZ3y6x5EIBELHxeOp5l0+0=
//study mac release 28JFZmC2Z4KeVOdRPzxtEuX8UJM=

public class AtchApplication extends Application {
    private volatile Activity currentActivity = null;
    private volatile ViewUpdateCallback viewUpdateCallback = null;

    private volatile Intent locationUpdateServiceIntent = null;
    private volatile Location currentLocation = null;
    private volatile Date lastUpdateTime = null;

    private volatile UserList friendsList = new UserList(User.UserType.FRIEND);


    public Activity getCurrentActivity() {
        return currentActivity;
    }
    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public void setViewUpdateCallback(ViewUpdateCallback viewUpdateCallback) {
        this.viewUpdateCallback = viewUpdateCallback;
    }
    public void updateView() {
        if (viewUpdateCallback != null){
            viewUpdateCallback.updateView();
        }
    }

    public void startLocationUpdates(){
        if(locationUpdateServiceIntent != null) return;
        locationUpdateServiceIntent = new Intent(this, LocationUpdateService.class);
        startService(locationUpdateServiceIntent);
    }
    public void stopLocationUpdates(){
        if(locationUpdateServiceIntent == null) return;
        stopService(locationUpdateServiceIntent);
        locationUpdateServiceIntent = null;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }
    public void setCurrentLocation(Location mCurrentLocation) {
        this.currentLocation = mCurrentLocation;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }
    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public UserList getFriendsList() {
        return friendsList;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //logReleaseHashKey();

        AtchParsePushReceiver.init(this);
        AtchParsePushReceiver.cancelAllNotifications(this);
        User.init(this);
        UserListAdapter.init(this);

        MapsInitializer.initialize(this);

        FacebookSdk.sdkInitialize(this);

        Parse.initialize(this, "P4g0harOzaQTi9g3QyEqGPI3HkiPJxxz4SJObhCE", "GpAM5yqJzbltLQENhwJt0cMbrVyM9q4aHR8O3k2s");
        ParseFacebookUtils.initialize(this);
    }

    private void logReleaseHashKey(){
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo("com.auriferous.atch", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.e("hash key", something);
            }
        }
        catch (Exception e) {
            Log.e("hash key", "error");
        }
    }

    //populates once results come in from Parse
    public void populateFriendList(){
        friendsList = new UserList(User.UserType.FRIEND);

        ParseQuery<ParseRole> roleQuery = ParseRole.getQuery();
        roleQuery.whereEqualTo("name", "friendsOf_" + ParseUser.getCurrentUser().getObjectId());
        roleQuery.getFirstInBackground(new GetCallback<ParseRole>() {
            @Override
            public void done(ParseRole role, ParseException e) {
                if (role == null) return;
                ParseRelation<ParseUser> relation = role.getRelation("users");
                ParseQuery<ParseUser> friendQuery = relation.getQuery();
                friendQuery.orderByAscending("fullname");
                friendQuery.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> list, ParseException e) {
                        for (ParseUser user : list)
                            friendsList.addUser(User.getOrCreateUser(user, User.UserType.FRIEND));

                        ParseAndFacebookUtils.updateFriendDataWithMostRecentLocations(friendsList, new FuncCallback<Object>() {
                            @Override
                            public void done(Object o) {
                                updateView();
                            }
                        });

                        updateView();
                    }
                });
            }
        });
    }
    public void addFriend(User newFriend){
        friendsList.addUser(newFriend);
    }
    public void removeFriend(User newEnemy){
        friendsList.removeUser(newEnemy);
    }
}