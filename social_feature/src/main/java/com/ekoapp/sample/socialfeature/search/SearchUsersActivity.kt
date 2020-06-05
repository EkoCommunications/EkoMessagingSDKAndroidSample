package com.ekoapp.sample.socialfeature.search

import android.content.Context
import com.ekoapp.sample.core.base.list.RecyclerBuilder
import com.ekoapp.sample.core.base.viewmodel.SingleViewModelActivity
import com.ekoapp.sample.core.ui.extensions.coreComponent
import com.ekoapp.sample.core.ui.extensions.observeNotNull
import com.ekoapp.sample.core.ui.extensions.observeOnce
import com.ekoapp.sample.socialfeature.R
import com.ekoapp.sample.socialfeature.di.DaggerSocialActivityComponent
import com.ekoapp.sample.socialfeature.intents.openUserFeedsPage
import com.ekoapp.sample.socialfeature.users.view.UsersViewModel
import com.ekoapp.sample.socialfeature.users.view.list.EkoUsersAdapter
import kotlinx.android.synthetic.main.activity_search_users.*
import kotlinx.android.synthetic.main.fragment_users.recycler_users
import javax.inject.Inject

class SearchUsersActivity : SingleViewModelActivity<UsersViewModel>() {
    private lateinit var adapter: EkoUsersAdapter

    @Inject
    lateinit var context: Context

    override fun getLayout(): Int {
        return R.layout.activity_search_users
    }

    override fun bindViewModel(viewModel: UsersViewModel) {
        setupAppBar()
        renderList(viewModel)
        setupEvent(viewModel)
    }

    private fun setupAppBar() {
        appbar_search.setup(this, true)
    }

    private fun renderList(viewModel: UsersViewModel) {
        viewModel.search(appbar_search.keyword())
        viewModel.observeKeyword().observeNotNull(this, { keyword ->
            keyword_search.render(keyword = keyword)
            var newKeyword = keyword

            adapter = EkoUsersAdapter(this, viewModel)
            RecyclerBuilder(context = this, recyclerView = recycler_users)
                    .builder()
                    .build(adapter)
            viewModel.bindSearchUserList(newKeyword).observeOnce(this, {
                adapter.submitList(it)
                newKeyword = ""
            })
        })
    }

    private fun setupEvent(viewModel: UsersViewModel) {
        viewModel.observeUserPage().observeNotNull(this, this::openUserFeedsPage)
    }

    override fun getViewModelClass(): Class<UsersViewModel> {
        return UsersViewModel::class.java
    }

    override fun initDependencyInjection() {
        DaggerSocialActivityComponent
                .builder()
                .coreComponent(coreComponent())
                .build()
                .inject(this)
    }
}