package rs.highlande.app.tatatu.model.event

import rs.highlande.app.tatatu.model.Post

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
data class PostChangeEvent(val post: Post? = null, val position: Int? = null)