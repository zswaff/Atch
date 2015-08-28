package com.auriferous.atch.Users;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.auriferous.atch.Activities.MapActivity;
import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.GeneralUtils;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class UserListAdapter extends BaseAdapter {
    private static AtchApplication app;

    private final Context context;
    private ArrayList<UserListAdapterSection> sections;
    private String emptyMessage;

    private int leftPadding = 10;

    private HashSet<String> viewsAccessed = new HashSet<>();
    private HashMap<String, View> allViews = new HashMap<>();


    public static void init(AtchApplication app){
        UserListAdapter.app = app;
    }


    public UserListAdapter(Context context, UserListAdapterSection section, String emptyMessage, UserListAdapter oldAdapter) {
        this(context, new ArrayList<>(Arrays.asList(new UserListAdapterSection[] {section})), emptyMessage, oldAdapter);
    }
    public UserListAdapter(Context context, ArrayList<UserListAdapterSection> sections, String emptyMessage, UserListAdapter oldAdapter) {
        this.context = context;
        this.sections = sections;
        this.emptyMessage = emptyMessage;

        leftPadding = GeneralUtils.convertDpToPixel(leftPadding, context);

        if(oldAdapter != null)
            allViews = oldAdapter.allViews;
    }


    @Override
    public int getCount() {
        int count = 0;
        for(UserListAdapterSection section : sections) {
            int size = section.getUsers().getAllUsers().size();
            if(size != 0 && section.getLabel() != null) count++;
            count += size;
        }
        if(count == 0) return 1;
        return count;
    }
    @Override
    public User getItem(int position) {
        for(UserListAdapterSection section : sections){
            ArrayList<User> users = section.getUsers().getAllUsers();

            if (users.size() == 0) continue;
            if(section.getLabel() != null) {
                if (position == 0) return null;
                position--;
            }

            if(position < users.size())
                return users.get(position);
            position -= users.size();
        }
        return null;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        for(UserListAdapterSection section : sections){
            ArrayList<User> users = section.getUsers().getAllUsers();

            if (users.size() == 0) continue;
            if(section.getLabel() != null) {
                if (position == 0)
                    return createLabelView(section.getLabel(), parent);
                position--;
            }

            if(position < users.size()) {
                String uid = users.get(position).getId();
                if(!viewsAccessed.contains(uid)){
                    viewsAccessed.add(uid);
                    if(allViews.containsKey(uid))
                        return allViews.get(uid);
                }
                View v = createUserView(users.get(position), parent);
                allViews.put(uid, v);
                return v;
            }
            position -= users.size();
        }
        if (position == 0)
            return createFullscreenLabelView(parent);
        return null;
    }


    private View createFullscreenLabelView(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.fullscreen_label, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.label);
        label.setText(emptyMessage);

        return rowView;
    }
    private View createLabelView(String text, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.label, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.label);
        label.setText(text);

        return rowView;
    }

    private View createUserView(final User user, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int layout = 0;
        switch(user.getUserType()){
            case FRIEND:
                layout = R.layout.friend_list_item;
                break;
            case PENDING_YOU:
                layout = R.layout.pending_you_list_item;
                break;
            case PENDING_THEM:
                layout = R.layout.pending_them_list_item;
                break;
            case FACEBOOK_FRIEND:
            case RANDOM:
                layout = R.layout.random_list_item;
                break;
        }
        View rowView = inflater.inflate(layout, parent, false);

        TextView fullname = (TextView) rowView.findViewById(R.id.fullname);
        TextView username = (TextView) rowView.findViewById(R.id.username);

        fullname.setText(user.getFullname());
        username.setText(user.getUsername());

        Bitmap profPic = user.getProfPic();
        if(profPic != null) {
            ImageView imageView = (ImageView) rowView.findViewById(R.id.prof_pic);
            imageView.setImageBitmap(profPic);
        }

        final String uid = user.getId();
        ImageButton chatButton = (ImageButton) rowView.findViewById(R.id.chat_button);
        if(chatButton != null)
            chatButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(context, MapActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra("type", "message");
                    intent.putExtra("chatterParseId", user.getId());
                    intent.putExtra("back", true);
                    app.getCurrentActivity().startActivity(intent);
                    app.getCurrentActivity().overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
                }
            });
        ImageButton friendButton = (ImageButton) rowView.findViewById(R.id.friend_button);
        if(friendButton != null)
            friendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.sendFriendRequest(uid);
                    user.setUserType(User.UserType.PENDING_THEM);
                    app.updateView();
                }
            });
        ImageButton acceptButton = (ImageButton) rowView.findViewById(R.id.accept_button);
        if(acceptButton != null)
            acceptButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.acceptFriendRequest(uid);
                    user.setUserType(User.UserType.FRIEND);
                    app.addFriend(user);
                    app.updateView();
                }
            });


        Button unfriendButton = (Button) rowView.findViewById(R.id.unfriend_button);
        if(unfriendButton != null)
            unfriendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.unfriendFriend(uid);
                    user.setUserType(User.UserType.RANDOM);
                    app.removeFriend(user);
                    app.updateView();
                }
            });
        Button rejectButton = (Button) rowView.findViewById(R.id.reject_button);
        if(rejectButton != null)
            rejectButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.rejectFriendRequest(uid);
                    user.setUserType(User.UserType.RANDOM);
                    app.updateView();
                }
            });
        Button cancelButton = (Button) rowView.findViewById(R.id.cancel_button);
        if(cancelButton != null)
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.cancelFriendRequest(uid);
                    user.setUserType(User.UserType.RANDOM);
                    app.updateView();
                }
            });

        initListener(user, rowView);
        return rowView;
    }
    private void initListener(final User user, final View rowView) {
        final Context context = this.context;
        rowView.setOnTouchListener(new View.OnTouchListener(){
            float slop = ViewConfiguration.get(context).getScaledTouchSlop();

            public boolean allTheWayLeft = false;
            int initialX = 0;
            boolean newEvent = true;

            @Override
            public boolean onTouch(final View view, MotionEvent event) {
                final View viewToMove = view.findViewById(R.id.main_view);
                if (viewToMove == null) return true;
                final Button swipeButton = (Button) ((ViewGroup)view.findViewById(R.id.button_view)).getChildAt(0);
                int sButtonWidth = swipeButton.getMeasuredWidth();

                int currentX = (int) event.getX();
                int offset = currentX - initialX;
                if(offset > sButtonWidth)
                    offset = sButtonWidth;
                if(-offset > sButtonWidth)
                    offset = -sButtonWidth;

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        initialX = (int) event.getX();
                        newEvent = true;

                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        if (!allTheWayLeft && offset < -slop) {
                            viewToMove.setPadding(offset + leftPadding, 0, 0, 0);
                            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewToMove.getLayoutParams();
                            params.setMargins(0, 0, -offset, 0);
                            viewToMove.requestLayout();

                            if (offset == -sButtonWidth)
                                allTheWayLeft = true;

                            swipeButton.setEnabled(allTheWayLeft);
                        }

                        if (Math.abs(offset) > slop)
                            newEvent = false;

                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        if (allTheWayLeft && offset > -slop) {
                            allTheWayLeft = false;
                            swipeButton.setEnabled(allTheWayLeft);
                        }
                        else if (Math.abs(offset) < slop) {
                            if (user.getUserType() == User.UserType.FRIEND) {
                                Intent intent = new Intent(context, MapActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                intent.putExtra("type", "zoom");
                                intent.putExtra("chatterParseId", user.getId());
                                intent.putExtra("back", true);
                                app.getCurrentActivity().startActivity(intent);
                                app.getCurrentActivity().overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
                            }
                        }

                        if (!allTheWayLeft) {
                            ValueAnimator animator = ValueAnimator.ofInt(viewToMove.getPaddingLeft(), leftPadding);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                    int paddingAmount = (Integer) valueAnimator.getAnimatedValue();
                                    viewToMove.setPadding(paddingAmount, 0, 0, 0);
                                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewToMove.getLayoutParams();
                                    params.setMargins(0, 0, -paddingAmount + leftPadding, 0);
                                    viewToMove.requestLayout();
                                }
                            });
                            animator.setDuration(150);
                            animator.start();
                        }
                    }
                }
                return true;
            }
        });
    }
}