package rs.highlande.app.tatatu.model.event

/**
 * @param type Can be 0 for follower and 1 for following
 * @param newFollow If true will add to the count. If false will reduce it
 */
data class UserFollowEvent(val type: Int, val newFollow: Boolean = true)