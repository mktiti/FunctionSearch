package com.mktiti.fsearch.parser.integrationtest

import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeHolder
import com.mktiti.fsearch.util.mapMutablePrefixTree

object RepoTestUtil {

    fun minimalRepos(infoRepo: JavaInfoRepo): Pair<JavaRepo, TypeResolver> {
        val directs = mapMutablePrefixTree<String, DirectType>().apply {
            fun insert(type: DirectType) {
                mutableSubtreeSafe(type.info.packageName)[type.info.simpleName] = type
            }

            val root = DirectType(infoRepo.objectType, false, emptyList(), null)
            insert(root)
            PrimitiveType.values().forEach { primitive ->
                insert(DirectType(infoRepo.boxed(primitive), false, TypeHolder.staticDirects(root), null))
            }
        }

        val typeRepo: TypeRepo = SetTypeRepo().apply {
            directs.forEach { direct ->
                this += direct
            }
        }

        return DefaultJavaRepo.fromRadix(infoRepo, directs) to SingleRepoTypeResolver(typeRepo)
    }

}