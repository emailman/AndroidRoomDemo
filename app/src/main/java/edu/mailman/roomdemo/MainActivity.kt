package edu.mailman.roomdemo

import android.app.AlertDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import edu.mailman.roomdemo.databinding.ActivityMainBinding
import edu.mailman.roomdemo.databinding.DialogUpdateBinding
// import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        val employeeDAO = (application as EmployeeApp).db.employeeDAO()

        binding?.btnAddRecord?.setOnClickListener {
            addRecord(employeeDAO)
        }

        lifecycleScope.launch {
            employeeDAO.fetchAllEmployees().collect {
                val list = ArrayList(it)
                setupListOfDataIntoRecyclerView(list, employeeDAO)
            }
        }
    }

    private fun addRecord(employeeDAO: EmployeeDAO) {
        val name = binding?.etName?.text.toString()
        val email = binding?.etEmailId?.text.toString()

        if (name.isNotEmpty() && email.isNotEmpty()) {
            lifecycleScope.launch {
                employeeDAO.insert(EmployeeEntity(name = name, email = email))
                Toast.makeText(
                    applicationContext, "Record saved",
                    Toast.LENGTH_LONG
                ).show()
                binding?.etName?.text?.clear()
                binding?.etEmailId?.text?.clear()
            }
        } else {
            Toast.makeText(
                applicationContext, "Must provide name and email",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupListOfDataIntoRecyclerView(
        employeesList: ArrayList<EmployeeEntity>, employeeDAO: EmployeeDAO
    ) {
        if (employeesList.isNotEmpty()) {
            val itemAdapter = ItemAdapter(employeesList,
                { updateId ->
                    updateRecordDialog(updateId, employeeDAO)
                })
            { deleteId ->
                lifecycleScope.launch {
                    employeeDAO.fetchEmployeesByID(deleteId).collect {
                        if (it != null) {
                            deleteRecordAlertDialog(deleteId, employeeDAO, it)
                        }
                    }
                }
            }

            binding?.rcvItemsList?.layoutManager = LinearLayoutManager(this)
            binding?.rcvItemsList?.adapter = itemAdapter
            binding?.rcvItemsList?.visibility = View.VISIBLE
            binding?.tvNoRecordsAvailable?.visibility = View.GONE
        } else {
            binding?.rcvItemsList?.visibility = View.GONE
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
        }
    }

    private fun updateRecordDialog(id: Int, employeeDAO: EmployeeDAO) {
        val updateDialog = Dialog(this, R.style.Theme_Dialog)

        updateDialog.setCancelable(false)
        val binding = DialogUpdateBinding.inflate(layoutInflater)
        updateDialog.setContentView(binding.root)

        lifecycleScope.launch {
            employeeDAO.fetchEmployeesByID(id).collect {
                if (it != null) {
                    // fill the employee name in the dialog
                    binding.etUpdateName.setText(it.name)

                    // fill the employee email in the dialog
                    binding.etUpdateEmailId.setText(it.email)
                }
            }
        }

        binding.tvUpdate.setOnClickListener {
            // Get employee name and email from dialog
            val name = binding.etUpdateName.text.toString()
            val email = binding.etUpdateEmailId.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty()) {
                lifecycleScope.launch {
                    // Update the employee record
                    employeeDAO.update(EmployeeEntity(id, name, email))
                    Toast.makeText(
                        applicationContext, "Record Updated",
                        Toast.LENGTH_LONG
                    ).show()
                    updateDialog.dismiss()
                }
            } else {
                Toast.makeText(
                    applicationContext, "Name and email must be provided",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.tvCancel.setOnClickListener {
            updateDialog.dismiss()
        }

        updateDialog.show()
    }

    private fun deleteRecordAlertDialog(
        id: Int,
        employeeDAO: EmployeeDAO,
        employee: EmployeeEntity
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Employee Record")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage("Are you sure you want to delete ${employee.name}?")

        builder.setPositiveButton("Yes") { dialogInterface, _ ->
            lifecycleScope.launch {
                employeeDAO.delete(EmployeeEntity(id))
                Toast.makeText(
                    applicationContext, "Record deleted successfully",
                    Toast.LENGTH_LONG
                ).show()
            }
            dialogInterface.dismiss()
        }
        builder.setNegativeButton("No") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}