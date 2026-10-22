package emu.nebula.game.activity.type;

import dev.morphia.annotations.Entity;

import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.activity.ActivityManager;

/**
 * Sign-in activity
 * <p>
 * The client does not distinguish sign-in activities from login reward activities, both
 * activity types are handled by the same login reward data class, they share the same
 * protocol message (ActivityLogin) and they are both claimed with the same request.
 * <p>
 * Because of that this activity only exists to give sign-in activities their own
 * activity type, all of the actual logic is inherited from the login reward activity.
 */
@Entity
public class SignInActivity extends LoginRewardActivity {
    
    @Deprecated // Morphia only
    public SignInActivity() {
        
    }
    
    public SignInActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
    }
    
}
