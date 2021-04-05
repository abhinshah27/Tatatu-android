# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile



# ===== FABRIC ====== #

-keepattributes *Annotation*
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# ===== FABRIC ====== #


##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Enums are kept
-keepclassmembers enum * { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class rs.highlande.tatatu.model.** { *; }
-keep class rs.highlande.app.tatatu.connection.http.HTTPResponse { *; }
-keep class rs.highlande.app.tatatu.connection.webSocket.SocketResponse { *; }

-keep class rs.highlande.app.tatatu.model.createPost.uploadImage.Data { *; }
-keep class rs.highlande.app.tatatu.model.createPost.uploadImage.UploadMediaResponse { *; }
-keep class rs.highlande.app.tatatu.model.createPost.CreatePostRequest { *; }
-keep class rs.highlande.app.tatatu.model.createPost.CreatePostMediaItemsRequest { *; }
-keep class rs.highlande.app.tatatu.model.createPost.Size { *; }


# INFO      commented because probably useless
#-keep class rs.highlande.app.tatatu.model.event.EditPostEvent { *; }
#-keep class rs.highlande.app.tatatu.model.event.EquivalentEvent { *; }
#-keep class rs.highlande.app.tatatu.model.event.ImageBottomEvent { *; }
#-keep class rs.highlande.app.tatatu.model.event.NotificationEvent { *; }
#-keep class rs.highlande.app.tatatu.model.event.PostChangeEvent { *; }
#-keep class rs.highlande.app.tatatu.model.event.UserFollowEvent { *; }

-keep class rs.highlande.app.tatatu.model.HomeNavigationData { *; }

-keep class rs.highlande.app.tatatu.model.InviteFriendsResponse { *; }
-keep class rs.highlande.app.tatatu.model.FriendsResponse { *; }
-keep class rs.highlande.app.tatatu.model.Users { *; }
-keep class rs.highlande.app.tatatu.model.MainUser { *; }
-keep class rs.highlande.app.tatatu.model.BalanceUser { *; }
-keep class rs.highlande.app.tatatu.model.FriendsUser { *; }
-keep class rs.highlande.app.tatatu.model.NotificationResponse { *; }
-keep class rs.highlande.app.tatatu.model.UserNotification { *; }
-keep class rs.highlande.app.tatatu.model.NotificationSimpleResponse { *; }

-keep class rs.highlande.app.tatatu.model.PostMediaItem { *; }
-keep class rs.highlande.app.tatatu.model.Size { *; }
-keep class rs.highlande.app.tatatu.model.Post { *; }
-keep class rs.highlande.app.tatatu.model.PostComment { *; }

-keep class rs.highlande.app.tatatu.model.StreamingCategory { *; }
-keep class rs.highlande.app.tatatu.model.StreamingVideo { *; }


-keep class rs.highlande.app.tatatu.model.TTUPlaylist { *; }
-keep class rs.highlande.app.tatatu.model.TTUVideo { *; }
-keep class rs.highlande.app.tatatu.model.TTUVideoFields { *; }

-keep class rs.highlande.app.tatatu.model.MainUserInfo { *; }
-keep class rs.highlande.app.tatatu.model.User { *; }
-keep class rs.highlande.app.tatatu.model.BalanceUserInfo { *; }
-keep class rs.highlande.app.tatatu.model.DetailUserInfo { *; }
-keep class rs.highlande.app.tatatu.model.PrivateUserInfo { *; }

-keep class rs.highlande.app.tatatu.model.chat.ChatMessage { *; }
-keep class rs.highlande.app.tatatu.model.chat.ChatRoom { *; }
-keep class rs.highlande.app.tatatu.model.chat.Participant { *; }
-keep class rs.highlande.app.tatatu.model.chat.HLWebLink { *; }



# INFO:     these classes work fine because they are CLASSES in model package
#-keep class rs.highlande.app.tatatu.model.BlockAccountDataList { *; }
#-keep class rs.highlande.app.tatatu.model.InvitationLinkResponse { *; }
#-keep class rs.highlande.app.tatatu.model.SendInvitationResponse { *; }
#-keep class rs.highlande.app.tatatu.model.SuggestedPerson { *; }


#This is extra - added by me to exclude gson obfuscation
-keep class com.google.gson.** { *; }
##---------------End: proguard configuration for Gson  ----------


# ===== EventBus ===== #

-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# ===== EventBus ===== #


# ===== Brightcove ===== #

-keep public class com.brightcove.player.** { public *;}
-keepclassmembers public class com.brightcove.player.** { public *;}
-keepclasseswithmembers public class com.brightcove.player.** { public *;}
-keep class com.google.android.exoplayer.** { *;}
-keep class com.brightcove.iab.** { *; }
-keep class com.google.** { *; }
-keep interface com.google.** { *; }
-keep class com.google.ads.interactivemedia.** { *; }
-keep interface com.google.ads.interactivemedia.** { *; }

# ===== Brightcove ===== #


# ===== RETROFIT ===== #

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# ===== RETROFIT ====== #


# ====== GLIDE ====== #

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# ====== GLIDE ====== #


# ====== COMSCORE ====== #

-keep class com.comscore.** { *; }
-dontwarn com.comscore.**

# ====== COMSCORE ====== #


# ====== twilio ====== #

-keep class tvi.webrtc.** { *; }
-keep class com.twilio.video.** { *; }
-keepattributes InnerClasses

# ====== twilio ====== #