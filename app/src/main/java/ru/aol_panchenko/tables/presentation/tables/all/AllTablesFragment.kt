package ru.aol_panchenko.tables.presentation.tables.all

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.Fragment
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.tables_fragment.*
import org.jetbrains.anko.support.v4.toast
import ru.aol_panchenko.tables.R
import ru.aol_panchenko.tables.presentation.model.Table
import ru.aol_panchenko.tables.presentation.tables.my.TablesAdapter
import ru.aol_panchenko.tables.presentation.tables.my.OnItemClickListener

/**
 * Created by alexey on 02.09.17.
 */
class AllTablesFragment : Fragment(), AllTablesMVPView, OnItemClickListener {

    private val REQUEST_PHONE: Int = 1
    private var _presenter: AllTablesPresenter? = null
    private var _adapter: TablesAdapter? = null
    private lateinit var _errorContainer: FrameLayout

    companion object {
        fun newInstance() = AllTablesFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.tables_fragment, container, false)
        _errorContainer = view.findViewById(R.id.nothing_container)
        _adapter = TablesAdapter(activity, this)
        _presenter = AllTablesPresenter(this)

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvTables.adapter = _adapter
        rvTables.layoutManager = LinearLayoutManager(activity)
    }

    override fun changeTable(table: Table) {
        _adapter?.changeItem(table)
    }

    override fun addTable(table: Table) {
        _adapter?.addItem(table)
    }

    override fun removeTable(table: Table) {
        _adapter?.removeItem(table)
    }

    override fun notifyListChangedNoConnection() {
        _adapter?.notifyDataClear()
    }

    override fun showItemMenu(view: View, table: Table) {
        val popupMenu = PopupMenu(activity, view)
        popupMenu.inflate(R.menu.menu_all_tables)
        popupMenu.setOnMenuItemClickListener { item -> when(item.itemId){
            R.id.item_menu_download -> {
                _presenter!!.onDownloadMenuClick(table)
                return@setOnMenuItemClickListener true
            }
            R.id.item_menu_sharing -> {
                _presenter!!.onSharingMenuClick(table)
                return@setOnMenuItemClickListener true
            }
            else -> {
                return@setOnMenuItemClickListener false
            }
        } }
        popupMenu.show()
    }

    override fun showContentState() {
        _errorContainer.visibility = View.GONE
    }

    override fun showErrorNetworkState() {
        _errorContainer.visibility = View.VISIBLE
    }

    override fun showErrorYourTable() {
        toast("У вас уже есть эта таблица :)")
    }

    override fun onItemClick(view: View, table: Table) {
        _presenter!!.onItemClick(view, table)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.serach_menu, menu)
        val searchItem = menu?.findItem(R.id.menu_search_action)
        val searchView = MenuItemCompat.getActionView(searchItem) as SearchView
        searchView.queryHint = getString(R.string.search_view_hint)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                _presenter?.onSearchQuerySubmit(newText)
                return true
            }
        })


        searchItem!!.setOnActionExpandListener(object : MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(p0: MenuItem?) = true

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                _presenter?.onSearchClosed()
                return true
            }
        })
    }

    override fun setTables(searchTables: ArrayList<Table>) {
        _adapter!!.setItems(searchTables)
    }

    override fun closeSearch() {
        activity.recreate()
    }

    override fun extractContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, REQUEST_PHONE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_PHONE) {
            val contactUri = data!!.data
            val queryFields = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val c = activity.contentResolver.query(contactUri, queryFields, null, null, null)
            if (c.moveToFirst()) {
                val indexPhone = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val number = c.getString(indexPhone)
                _presenter!!.onContactExtracted(number)
            }
            c.close()
        }
    }

    override fun showAlreadyExistMessage() {
        toast(getString(R.string.user_already_has_table_message))
    }

    override fun showNotInstallMessage() {
        toast(getString(R.string.user_has_not_app_message))
    }
}