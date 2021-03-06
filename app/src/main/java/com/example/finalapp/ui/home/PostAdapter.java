package com.example.finalapp.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalapp.detail.DetailActivity;
import com.example.finalapp.R;
import com.example.finalapp.model.Post;
import com.example.finalapp.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    public Context mContext;
    public List<Post> mPost;

    private FirebaseUser firebaseUser;

    public PostAdapter(Context mContext, List<Post> mPost) {
        this.mContext = mContext;
        this.mPost = mPost;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.post_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        final Post post = mPost.get(position);

        if (post.getImages() == null || post.getImages().size() == 0) {
            holder.postImage.setVisibility(View.GONE);
        } else {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(mContext).load(post.getImages().get(0)).into(holder.postImage);
        }

        holder.description.setText(post.getDetail().replaceAll("<br />", "\n"));
        holder.postTitle.setText(post.getTitle());
        holder.price.setText("$ " + Double.toString(post.getPrice()));

        posterInfo(holder.posterImg, holder.poster, holder.posterRate, post.getPoster());
        isAddShop(post.getPostid(), holder.addToShop);

        holder.addToShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.addToShop.getTag().equals("add")) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    Map<String, Object> add = new HashMap<>();
                    add.put("quantity", 1);
                    add.put("postid", post.getPostid());
                    add.put("totalPrice", post.getPrice());
                    db.collection("shop").document(firebaseUser.getUid())
                            .collection("posts").document(post.getPostid())
                            .set(add, SetOptions.merge())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    holder.addToShop.setImageResource(R.drawable.ic_baseline_check_circle_24);
                                    holder.addToShop.setTag("added");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(mContext, "Fail to add", Toast.LENGTH_SHORT).show();
                                }
                            });

                } else {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("shop").document(firebaseUser.getUid())
                            .collection("posts").document(post.getPostid())
                            .delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    holder.addToShop.setImageResource(R.drawable.ic_baseline_add_shopping_cart_24);
                                    holder.addToShop.setTag("add");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(mContext, "Fail to remove", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

        holder.postTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DetailActivity.class);
                intent.putExtra("postid", post.getPostid());
                mContext.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mPost.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView postImage, posterImg, addToShop;
        public TextView postTitle, poster, posterRate, description, price;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            postImage = itemView.findViewById(R.id.postImage);
            posterImg = itemView.findViewById(R.id.posterImg);
            addToShop = itemView.findViewById(R.id.addToShop);
            postTitle = itemView.findViewById(R.id.postTitle);
            poster = itemView.findViewById(R.id.poster);
            posterRate = itemView.findViewById(R.id.posterRate);
            description = itemView.findViewById(R.id.description);
            price = itemView.findViewById(R.id.postPrice);
        }

    }

    private void posterInfo(final ImageView posterImg, final TextView poster, final TextView posterRate, String userid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        Glide.with(mContext).load(user.getImageurl()).into(posterImg);
                        poster.setText(user.getUsername());
                        DecimalFormat df = new DecimalFormat("#.#");
                        posterRate.setText(df.format(user.getRate()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void isAddShop(final String postid, final ImageView addToShop) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("shop").document(firebaseUser.getUid())
                .collection("posts").document(postid)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(mContext, "Fail to get shop list", Toast.LENGTH_SHORT).show();
                        }
                        if (value.exists()) {
                            addToShop.setImageResource(R.drawable.ic_baseline_check_circle_24);
                            addToShop.setTag("added");
                        } else {
                            addToShop.setImageResource(R.drawable.ic_baseline_add_shopping_cart_24);
                            addToShop.setTag("add");

                        }
                    }
                });
    }
}
