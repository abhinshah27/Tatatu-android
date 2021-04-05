package rs.highlande.app.tatatu.model

/**
 * TODO - Interface description
 * @author mbaldrighi on 2019-07-17.
 */
interface BaseDiffutilImpl<in T> {

    fun areItemsTheSame(other: T): Boolean
    fun areContentsTheSame(other: T): Boolean

}