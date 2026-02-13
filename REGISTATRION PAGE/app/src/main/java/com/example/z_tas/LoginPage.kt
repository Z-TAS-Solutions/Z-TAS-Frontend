class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)

        // Switch to Register Page
        tvToRegister.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close login so 'back' button doesn't loop forever
        }

        // Login Action
        btnLogin.setOnClickListener {
            // Add your validation logic here!
            Toast.makeText(this, "Attempting Secure Login...", Toast.LENGTH_SHORT).show()
        }
    }
}